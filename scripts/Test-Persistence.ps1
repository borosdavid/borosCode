param([string]$MySqlBin)
$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot)
$projectRoot = (Get-Location).Path
if (!$MySqlBin) { $MySqlBin = Join-Path $projectRoot '.local/mysql-9.7.4-winx64/bin' }
$server = Join-Path $MySqlBin 'mysqld.exe'
$client = Join-Path $MySqlBin 'mysql.exe'
$admin = Join-Path $MySqlBin 'mysqladmin.exe'
foreach ($tool in @($server, $client, $admin)) {
    if (!(Test-Path -LiteralPath $tool)) { throw "Hiányzó MySQL teszteszköz: $tool" }
}
$maven = if (Test-Path '.local/apache-maven-3.10.0/bin/mvn.cmd') {
    (Resolve-Path '.local/apache-maven-3.10.0/bin/mvn.cmd').Path
} else { (Get-Command mvn.cmd -ErrorAction Stop).Source }
if (Test-Path '.local/jdk-25.0.5/bin/javac.exe') {
    $env:JAVA_HOME = (Resolve-Path '.local/jdk-25.0.5').Path
}
$testRoot = Join-Path $projectRoot ('target/jpa-it/' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $testRoot -Force | Out-Null
$dataDirectory = Join-Path $testRoot 'mysql'
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$listener.Start()
$testPort = $listener.LocalEndpoint.Port
$listener.Stop()
$mysqlProcess = $null
try {
    & $server --no-defaults --initialize-insecure "--datadir=$dataDirectory" *> "$testRoot/initialize.log"
    if ($LASTEXITCODE) { throw "A tesztadatbázis inicializálása sikertelen: $testRoot/initialize.log" }
    $serverArguments = @('--no-defaults', ('--datadir="' + $dataDirectory + '"'),
        "--port=$testPort", '--bind-address=127.0.0.1', '--mysqlx=OFF', '--skip-log-bin', '--console')
    $mysqlProcess = Start-Process -FilePath $server -ArgumentList $serverArguments -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput "$testRoot/mysql.log" -RedirectStandardError "$testRoot/mysql.err"
    $ready = $false
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ($mysqlProcess.HasExited) { throw "A teszt MySQL leállt: $testRoot/mysql.err" }
        & $admin --no-defaults --protocol=tcp '--host=127.0.0.1' "-P$testPort" -uroot --connect-timeout=1 ping *> $null
        if (!$LASTEXITCODE) { $ready = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (!$ready) { throw 'A különálló teszt MySQL nem vált elérhetővé.' }
    & $client --no-defaults --protocol=tcp '--host=127.0.0.1' "-P$testPort" -uroot `
        '--execute=CREATE DATABASE inventory_jpa_test CHARACTER SET utf8mb4;'
    if ($LASTEXITCODE) { throw 'A külön tesztséma létrehozása sikertelen.' }
    $arguments = @('-B', '-ntp', '-Dmaven.repo.local=.local/m2', '-Pmysql-it',
        "-Dtest.db.url=jdbc:mysql://127.0.0.1:$testPort/inventory_jpa_test",
        '-Dtest.db.user=root', 'verify')
    if (Test-Path '.local/settings.xml') { $arguments += @('-s', '.local/settings.xml') }
    & $maven @arguments 2>&1 | Tee-Object -FilePath "$testRoot/maven.log"
    if ($LASTEXITCODE) { throw "A JPA integrációs ellenőrzés hibás: $testRoot/maven.log" }
    Write-Host "Sikeres, elkülönített MySQL-ellenőrzés. Bizonyítékok: $testRoot"
} finally {
    if ($mysqlProcess -and !$mysqlProcess.HasExited) {
        & $admin --no-defaults --protocol=tcp '--host=127.0.0.1' "-P$testPort" -uroot --connect-timeout=3 shutdown *> $null
        if (!$mysqlProcess.WaitForExit(15000)) {
            $ownedProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($mysqlProcess.Id)"
            if ($ownedProcess -and $ownedProcess.CommandLine.Contains($dataDirectory)) {
                Stop-Process -Id $mysqlProcess.Id
            }
        }
    }
}

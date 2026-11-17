param([switch]$TestRun,[switch]$Stop,[switch]$NoBrowser,[switch]$Native,[switch]$DebugServices)
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot)
if($DebugServices -and !$TestRun){throw 'A hibakereséshez válaszd a -TestRun profilt.'}
$root=(Get-Location).Path
$toolsDir=Join-Path $root '.local'
$profileName=if($TestRun){'acceptance'}else{'personal'}
$state=Join-Path $toolsDir $profileName
New-Item -ItemType Directory -Force $state | Out-Null
$env:WEB_PORT=if($TestRun){'18740'}else{'8740'}
$env:IMAGE_PORT=if($TestRun){'18741'}else{'8741'}
$env:DB_PORT=if($TestRun){'43316'}else{'33316'}
$url='http://127.0.0.1:'+$env:WEB_PORT
$env:IMAGING_URL='http://127.0.0.1:'+$env:IMAGE_PORT
$env:PHOTO_DIR=Join-Path $state 'photos'
if(!$env:PUBLIC_ORIGIN -and !$env:SESSION_COOKIE_SECURE){$env:SESSION_COOKIE_SECURE='false'}
$secretFile=Join-Path $state 'access.json'
function Secret { [guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N') }
if(!(Test-Path $secretFile)){
    @{app=Secret;reader=Secret;root=Secret} | ConvertTo-Json | Set-Content -Encoding ascii $secretFile
}
$keys=Get-Content -Raw $secretFile | ConvertFrom-Json
$env:DB_PASSWORD=$keys.app
$env:READER_PASSWORD=$keys.reader
$env:MYSQL_ROOT_PASSWORD=$keys.root
$env:COMPOSE_PROJECT_NAME='otthontar-'+$profileName
function Healthy($address) {
    try { (Invoke-RestMethod "$address/actuator/health" -TimeoutSec 3).status -eq 'UP' } catch { $false }
}
function WaitFor($address){
    for($i=0;$i -lt 90;$i++){if(Healthy $address){return};Start-Sleep -Seconds 1}
    throw "Az alkalmazás nem indult el: $address. Naplók: $state"
}
function Launch($exe,$arguments,$name){
    $proc=Start-Process -FilePath $exe -ArgumentList $arguments -PassThru -WindowStyle Hidden -RedirectStandardOutput "$state/$name.log" -RedirectStandardError "$state/$name.err"
    $proc.Id | Set-Content "$state/$name.pid"
}
function StopOwned($name){
    $file="$state/$name.pid"
    if(Test-Path $file){
        $processId=[int](Get-Content $file)
        $process=Get-CimInstance Win32_Process -Filter "ProcessId=$processId"
        if($process -and $process.CommandLine.Contains($state)){Stop-Process -Id $processId}
    }
}
$modeFile="$state/runtime.txt"
$mode=if(Test-Path $modeFile){Get-Content $modeFile}else{'native'}
if($Stop){
    if($mode -eq 'docker'){docker compose stop;if($LASTEXITCODE){throw 'Docker leállítási hiba.'}}
    else{
        StopOwned 'inventory';StopOwned 'imaging'
        $env:MYSQL_PWD=$keys.root
        & "$toolsDir/mysql-9.7.4-winx64/bin/mysqladmin.exe" --protocol=tcp '--host=127.0.0.1' "-P$env:DB_PORT" -uroot shutdown
        if($LASTEXITCODE){throw 'A MySQL leállítása nem sikerült.'}
        Remove-Item Env:MYSQL_PWD
    }
    return
}
if(!(Healthy $url)){
    if(!(Test-Path $modeFile) -and !$Native -and (Get-Command docker -ErrorAction SilentlyContinue)){
        docker info --format '{{.ServerVersion}}' 2>$null | Out-Null
        if($LASTEXITCODE -eq 0){docker compose version | Out-Null;if($LASTEXITCODE -eq 0){$mode='docker'}}
    }
    $mode | Set-Content $modeFile
    if($mode -eq 'docker'){
        docker compose up -d --build
        if($LASTEXITCODE){throw 'A Docker indítása sikertelen. Indítsd el a Docker Desktopot, majd próbáld újra.'}
    }else{
        $mysqlBin="$toolsDir/mysql-9.7.4-winx64/bin"
        $java=(Get-Command java.exe -ErrorAction SilentlyContinue).Source
        if(Test-Path 'C:/Program Files/Java/jdk-21/bin/java.exe'){$java='C:/Program Files/Java/jdk-21/bin/java.exe'}
        if(Test-Path "$toolsDir/jdk-25.0.5/bin/java.exe"){ $java="$toolsDir/jdk-25.0.5/bin/java.exe" }
        if(!$java -or !(Test-Path "$mysqlBin/mysqld.exe") -or !(Test-Path inventory/target/inventory-1.0.0.jar)){
            throw 'A helyi futtatókörnyezet hiányzik. Kövesd a README Windowsos beállítási lépéseit.'
        }
        $dbDir="$state/mysql"
        $init="$state/first-start.sql"
        if(!(Test-Path "$dbDir/mysql")){
            New-Item -ItemType Directory -Force $dbDir | Out-Null
            & "$mysqlBin/mysqld.exe" --no-defaults --initialize-insecure "--datadir=$dbDir"
            if($LASTEXITCODE){throw 'A MySQL inicializálása sikertelen.'}
            @("ALTER USER 'root'@'localhost' IDENTIFIED BY '$($keys.root)';","CREATE DATABASE inventory CHARACTER SET utf8mb4;","CREATE USER 'inventory'@'localhost' IDENTIFIED BY '$($keys.app)';","GRANT ALL ON inventory.* TO 'inventory'@'localhost';","CREATE USER 'viewer'@'localhost' IDENTIFIED BY '$($keys.reader)';","GRANT SELECT ON inventory.* TO 'viewer'@'localhost';") | Set-Content -Encoding ascii $init
        }
        $env:MYSQL_PWD=$keys.root
        $admin="$mysqlBin/mysqladmin.exe"
        $connection=@('--protocol=tcp','-h127.0.0.1',"-P$env:DB_PORT",'-uroot')
        $ErrorActionPreference='Continue'
        & $admin @connection --connect-timeout=1 ping 2>$null | Out-Null
        if($LASTEXITCODE){
            $arguments=@('--no-defaults',(' --datadir="'+$dbDir+'"').Trim(),"--port=$env:DB_PORT",'--bind-address=127.0.0.1','--mysqlx=OFF','--console')
            if(Test-Path $init){$arguments+=('--init-file="'+$init+'"')}
            Launch "$mysqlBin/mysqld.exe" $arguments 'mysql'
            for($i=0;$i -lt 60;$i++){Start-Sleep -Seconds 1;& $admin @connection ping 2>$null | Out-Null;if(!$LASTEXITCODE){break}}
        }
        $ErrorActionPreference='Stop'
        if($LASTEXITCODE){throw "A MySQL nem elérhető. Lásd: $state/mysql.err"}
        if(Test-Path $init){Remove-Item -LiteralPath $init}
        Remove-Item Env:MYSQL_PWD
        foreach($service in @('imaging','inventory')){
            if($service -eq 'imaging' -and (Healthy $env:IMAGING_URL)){continue}
            $runtimeJar=Join-Path $state ($service+'.jar')
            Copy-Item -LiteralPath "$root/$service/target/$service-1.0.0.jar" -Destination $runtimeJar -Force
            $jvmOptions=@()
            if($DebugServices) {
                $debugPort=if($service -eq 'inventory'){18840}else{18841}
                $jvmOptions+=("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:"+$debugPort)
            }
            $jvmOptions+=@('-Xmx256m','-Djava.awt.headless=true',('-Dotthontar.state="'+$state+'"'),'-jar',('"'+$runtimeJar+'"'))
            Launch $java $jvmOptions $service
            if($service -eq 'imaging'){WaitFor $env:IMAGING_URL}
        }
    }
    WaitFor $url
}
Write-Host "Az Otthontár elindult: $url/leltar"
if(!$NoBrowser){Start-Process "$url/leltar"}

Set-StrictMode -Version 2
$ErrorActionPreference='Stop'

function Write-AtomicJson {
    param([string]$Path, $Value)
    $directory=Split-Path -Parent $Path
    [IO.Directory]::CreateDirectory($directory) | Out-Null
    $temporary=Join-Path $directory ([guid]::NewGuid().ToString('N')+'.tmp')
    $bytes=[Text.UTF8Encoding]::new($false).GetBytes(($Value | ConvertTo-Json -Depth 12 -Compress))
    try {
        $stream=[IO.File]::Open($temporary,[IO.FileMode]::CreateNew,[IO.FileAccess]::Write,[IO.FileShare]::None)
        try {$stream.Write($bytes,0,$bytes.Length);$stream.Flush($true)}finally{$stream.Dispose()}
        if([IO.File]::Exists($Path)){
            $backup=$temporary+'.bak'
            try{[IO.File]::Replace($temporary,$Path,$backup)}finally{if([IO.File]::Exists($backup)){[IO.File]::Delete($backup)}}
        }else{[IO.File]::Move($temporary,$Path)}
    }finally{if([IO.File]::Exists($temporary)){[IO.File]::Delete($temporary)}}
}

function Get-TestOrigin {
    param([string]$Root)
    $path=Join-Path $Root '.local/remote-test/https.json'
    if(!(Test-Path -LiteralPath $path)){return ''}
    $config=Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    $origin=[string]$config.public_origin
    if($origin -cnotmatch '^https://[a-z0-9][a-z0-9.-]*\.ts\.net$'){throw 'invalid_https_configuration'}
    return $origin
}

function Test-LocalHealth {
    param([int]$Port)
    try {return (Invoke-RestMethod -Uri "http://127.0.0.1:$Port/actuator/health" -TimeoutSec 3).status -eq 'UP'}catch{return $false}
}

function Test-LocalDatabase {
    $client=[Net.Sockets.TcpClient]::new()
    try {
        $connected=$client.ConnectAsync('127.0.0.1',43316)
        return $connected.Wait(1000) -and $client.Connected
    }catch{return $false}finally{$client.Dispose()}
}

function Get-TestServerStatus {
    param([string]$Root)
    $state=Join-Path $Root '.local/acceptance'
    $hashes=@{}
    foreach($service in @('inventory','imaging')){
        $path=Join-Path $state ($service+'.jar')
        $hashes[$service]=if(Test-Path -LiteralPath $path){(Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash.ToLowerInvariant()}else{$null}
    }
    $source=$null
    $manifest=Join-Path $Root '.local/build-info.json'
    if(Test-Path -LiteralPath $manifest){
        try{
            $info=Get-Content -LiteralPath $manifest -Raw | ConvertFrom-Json
            if($info.source_commit -cmatch '^[a-f0-9]{40}$' -and $info.inventory_sha256 -ceq $hashes.inventory -and $info.imaging_sha256 -ceq $hashes.imaging){$source=$info.source_commit}
        }catch{}
    }
    return [ordered]@{
        environment='teszt'; inventory_up=[bool](Test-LocalHealth 18740); imaging_up=[bool](Test-LocalHealth 18741)
        database_tcp_open=[bool](Test-LocalDatabase); source_commit=$source
        inventory_sha256=$hashes.inventory; imaging_sha256=$hashes.imaging
    }
}

function Assert-TestBuild {
    param([string]$Root)
    $path=Join-Path $Root '.local/build-info.json'
    if(!(Test-Path -LiteralPath $path)){throw 'verified_build_required'}
    $info=Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    if($info.source_commit -cnotmatch '^[a-f0-9]{40}$'){throw 'verified_build_required'}
    foreach($service in @('inventory','imaging')){
        $jar=Join-Path $Root "$service/target/$service-1.0.0.jar"
        $expected=$info.($service+'_sha256')
        if(!(Test-Path -LiteralPath $jar) -or $expected -cnotmatch '^[a-f0-9]{64}$' -or (Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash.ToLowerInvariant() -cne $expected){throw 'verified_build_required'}
    }
    Push-Location $Root
    try{
        & git diff --quiet $info.source_commit -- pom.xml inventory/src inventory/pom.xml imaging/src imaging/pom.xml
        if($LASTEXITCODE -ne 0){throw 'source_changed_since_build'}
        $untracked=@(& git ls-files --others --exclude-standard -- inventory/src imaging/src)
        if($LASTEXITCODE -ne 0 -or $untracked.Count -gt 0){throw 'source_changed_since_build'}
    }finally{Pop-Location}
}

function Stop-OwnedTestApplication {
    param([string]$Root)
    $state=Join-Path $Root '.local/acceptance'
    foreach($service in @('inventory','imaging')){
        $pidFile=Join-Path $state ($service+'.pid')
        if(!(Test-Path -LiteralPath $pidFile)){continue}
        $text=(Get-Content -LiteralPath $pidFile -Raw).Trim()
        if($text -notmatch '^[1-9][0-9]{0,9}$'){throw 'invalid_test_pid'}
        $owned=Get-CimInstance Win32_Process -Filter "ProcessId=$text"
        if($null -eq $owned){continue}
        $jar=Join-Path $state ($service+'.jar')
        if($owned.Name -notin @('java.exe','javaw.exe') -or !$owned.CommandLine -or !$owned.CommandLine.Contains($jar) -or !$owned.CommandLine.Contains('-Dotthontar.state=')){throw 'process_ownership_mismatch'}
        Stop-Process -Id ([int]$text) -ErrorAction Stop
    }
    # The isolated MySQL remains up; no database, photo or profile deletion is performed.
}

function Invoke-TestStartProcess {
    param([string]$Root)
    $start=Join-Path $Root 'scripts/Start.ps1'
    $shell=Join-Path $env:SystemRoot 'System32/WindowsPowerShell/v1.0/powershell.exe'
    $state=Join-Path $Root '.local/remote-test'
    $output=Join-Path $state 'start.log';$errorLog=Join-Path $state 'start.err'
    $arguments='-NoProfile -NonInteractive -ExecutionPolicy Bypass -File "'+$start+'" -TestRun -Native -NoBrowser'
    $process=Start-Process -FilePath $shell -ArgumentList $arguments -PassThru -WindowStyle Hidden -RedirectStandardOutput $output -RedirectStandardError $errorLog
    $process.Handle | Out-Null
    Write-AtomicJson (Join-Path $state 'starting.json') @{pid=$process.Id;started=$process.StartTime.ToUniversalTime().ToString('o')}
    if(!$process.WaitForExit(150000)){
        # Do not kill a descendant tree that may contain a newly started JVM/database.
        # Its state is uncertain; the durable agent journal prevents automatic replay.
        throw 'start_timeout_unknown'
    }
    Remove-Item -LiteralPath (Join-Path $state 'starting.json') -Force
    $process.Refresh()
    if($process.ExitCode -ne 0){throw 'start_failed'}
}

function Invoke-TestServer {
    param([Parameter(Mandatory=$true)][ValidateSet('status','diagnostics','start','stop','restart')][string]$Action,
        [Parameter(Mandatory=$true)][string]$Root)
    $Root=[IO.Path]::GetFullPath($Root)
    $directory=Join-Path $Root '.local/remote-test'
    [IO.Directory]::CreateDirectory($directory) | Out-Null
    $guard=$null
    try{
        try{$guard=[IO.File]::Open((Join-Path $directory 'control.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)}catch{throw 'control_busy'}
        $modeFile=Join-Path $Root '.local/acceptance/runtime.txt'
        if((Test-Path -LiteralPath $modeFile) -and (Get-Content -LiteralPath $modeFile -Raw).Trim() -ne 'native'){throw 'native_test_profile_required'}
        if($Action -in @('status','diagnostics')){return Get-TestServerStatus $Root}
        $starting=Join-Path $directory 'starting.json'
        if(Test-Path -LiteralPath $starting){
            $pending=Get-Content -LiteralPath $starting -Raw | ConvertFrom-Json
            $child=Get-Process -Id ([int]$pending.pid) -ErrorAction SilentlyContinue
            if($null -ne $child -and $child.StartTime.ToUniversalTime().ToString('o') -ceq $pending.started){throw 'previous_start_running'}
            Remove-Item -LiteralPath $starting -Force
        }
        if($Action -in @('stop','restart')){
            Stop-OwnedTestApplication $Root
            for($i=0;$i -lt 20;$i++){
                if(!(Test-LocalHealth 18740) -and !(Test-LocalHealth 18741)){break}
                Start-Sleep -Milliseconds 250
            }
            if((Test-LocalHealth 18740) -or (Test-LocalHealth 18741)){throw 'stop_not_confirmed'}
        }
        if($Action -in @('start','restart')){
            Assert-TestBuild $Root
            $origin=Get-TestOrigin $Root
            $runtimeInfo=Join-Path $directory 'runtime.json'
            $alreadyUp=Test-LocalHealth 18740
            if($alreadyUp){
                $expected=Get-Content -LiteralPath (Join-Path $Root '.local/build-info.json') -Raw | ConvertFrom-Json
                $actual=Get-TestServerStatus $Root
                if($actual.inventory_sha256 -cne $expected.inventory_sha256 -or $actual.imaging_sha256 -cne $expected.imaging_sha256 -or !$actual.imaging_up){throw 'restart_required'}
                if(!(Test-Path -LiteralPath $runtimeInfo) -or (Get-Content -LiteralPath $runtimeInfo -Raw | ConvertFrom-Json).public_origin -cne $origin){throw 'restart_required'}
            }else{
                # A previous failed/slow start may have left a JVM alive without health.
                # Never launch another copy; an explicit, ownership-checked restart is required.
                foreach($service in @('inventory','imaging')){
                    $pidFile=Join-Path $Root ('.local/acceptance/'+$service+'.pid')
                    if(Test-Path -LiteralPath $pidFile){
                        $processIdText=(Get-Content -LiteralPath $pidFile -Raw).Trim()
                        if($processIdText -notmatch '^[1-9][0-9]{0,9}$'){throw 'invalid_test_pid'}
                        if($null -ne (Get-CimInstance Win32_Process -Filter ('ProcessId='+$processIdText))){throw 'restart_required'}
                    }
                }
                $old=@{}
                foreach($key in @('PUBLIC_ORIGIN','SESSION_COOKIE_SECURE','BIND_ADDRESS','DB_HOST')){$old[$key]=[Environment]::GetEnvironmentVariable($key,'Process')}
                try{
                    $env:PUBLIC_ORIGIN=$origin;$env:SESSION_COOKIE_SECURE=if($origin){'true'}else{'false'};$env:BIND_ADDRESS='127.0.0.1';$env:DB_HOST='127.0.0.1'
                    Invoke-TestStartProcess $Root
                }finally{foreach($key in $old.Keys){[Environment]::SetEnvironmentVariable($key,$old[$key],'Process')}}
                if(!(Test-LocalHealth 18740) -or !(Test-LocalHealth 18741)){throw 'health_failed'}
                Write-AtomicJson $runtimeInfo @{public_origin=$origin}
            }
        }
        return Get-TestServerStatus $Root
    }finally{if($null -ne $guard){$guard.Dispose()}}
}

Export-ModuleMember -Function Write-AtomicJson,Get-TestOrigin,Get-TestServerStatus,Invoke-TestServer

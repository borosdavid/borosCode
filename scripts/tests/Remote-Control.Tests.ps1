$ErrorActionPreference='Stop'
Set-StrictMode -Version 2
$root=Split-Path (Split-Path $PSScriptRoot)
$script:passed=0
function Assert($Value,[string]$Name){if(!$Value){throw "FAIL: $Name"};$script:passed++}
function Reject([scriptblock]$Work,[string]$Code){try{& $Work;throw 'NO_REJECTION'}catch{Assert ($_.Exception.Message -ceq $Code) "$Code, actual=$($_.Exception.Message)"}}
$files=@(Get-ChildItem -LiteralPath (Join-Path $root 'scripts') -Recurse -File | Where-Object {$_.Extension -in @('.ps1','.psm1')})
foreach($file in $files){$errors=$null;$tokens=$null;[Management.Automation.Language.Parser]::ParseFile($file.FullName,[ref]$tokens,[ref]$errors) | Out-Null;Assert ($errors.Count -eq 0) "parse $($file.Name): $errors"}
Import-Module (Join-Path $root 'scripts/remote/Control.Core.psm1') -Force
Import-Module (Join-Path $root 'scripts/remote/Https.Core.psm1') -Force
Import-Module (Join-Path $root 'scripts/remote/TestServer.Core.psm1') -Force
$temp=Join-Path ([IO.Path]::GetTempPath()) ('otthontar-control-'+[guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($temp) | Out-Null
try{
    $now=[DateTimeOffset]::Parse('2026-11-17T10:00:00Z')
    $id=[guid]::NewGuid().ToString();$sha='a'*40
    $request=[ordered]@{schema_version=1;request_id=$id;server_id='windows-test';environment='teszt';operation='status';created_at='2026-11-17T10:00:00Z';expires_at='2026-11-17T10:05:00Z'}
    function Json {return ($request | ConvertTo-Json -Compress)}
    foreach($operation in @('status','diagnostics','start','stop','restart')){$request.operation=$operation;$parsed=Get-ControlRequest (Json) $id 'windows-test' $now;Assert ($parsed.operation -ceq $operation) "operation $operation"}
    $request.operation='status'
    Reject {Get-ControlRequest ((Json).Replace('"schema_version":1','"schema_version":1,"shell":"calc"')) $id 'windows-test' $now} 'rejected_schema'
    Reject {Get-ControlRequest ((Json).Replace('"schema_version":1','"schema_version":1,"schema_version":1')) $id 'windows-test' $now} 'rejected_schema'
    Reject {Get-ControlRequest '[]' $id 'windows-test' $now} 'rejected_schema'
    Reject {Get-ControlRequest ('x'*4097) $id 'windows-test' $now} 'rejected_schema'
    Reject {Get-ControlRequest (Json) '../bad' 'windows-test' $now} 'rejected_schema'
    Reject {Get-ControlRequest (Json) $id 'different-host' $now} 'rejected_target'
    $request.environment='personal';Reject {Get-ControlRequest (Json) $id 'windows-test' $now} 'rejected_target';$request.environment='teszt'
    foreach($operation in @('START','exec','deploy','delete','powershell')){$request.operation=$operation;Reject {Get-ControlRequest (Json) $id 'windows-test' $now} 'rejected_action'}
    $request.operation='status'
    Reject {Get-ControlRequest (Json) $id 'windows-test' $now.AddMinutes(6)} 'rejected_expired'
    Reject {Get-ControlRequest (Json) $id 'windows-test' $now.AddMinutes(-1)} 'rejected_expired'
    $request.expires_at='2026-11-17T10:11:00Z';Reject {Get-ControlRequest (Json) $id 'windows-test' $now} 'rejected_expired';$request.expires_at='2026-11-17T10:05:00Z'
    $script:executions=0;$script:publications=0
    $executor={param($action) $script:executions++;return @{inventory_up=$true;source_commit=('b'*40);token='secret';logs='sensitive'}}
    $publisher={param($requestId,$result) $script:publications++}
    $requestArgs=@{Json=(Json);RequestId=$id;RequestSha=$sha;ServerId='windows-test';StateDirectory=(Join-Path $temp 'journal');AllowMutations=$false;Executor=$executor;Publisher=$publisher;Now=$now}
    $result=Invoke-ControlRequest @requestArgs
    Assert ($result.state -eq 'succeeded' -and $script:executions -eq 1) 'first execution'
    Assert (($result.data | ConvertTo-Json) -notmatch 'secret|sensitive|token|logs') 'allowlist redaction'
    Assert ($result.data.inventory_up -eq $true -and $result.data.source_commit -eq ('b'*40)) 'safe fields retained'
    $result=Invoke-ControlRequest @requestArgs
    Assert ($script:executions -eq 1 -and $script:publications -eq 1) 'replay does not execute or publish'
    $requestArgs.RequestSha='b'*40;Reject {Invoke-ControlRequest @requestArgs} 'request_changed';$requestArgs.RequestSha=$sha
    $guard=[IO.File]::Open((Join-Path $requestArgs.StateDirectory 'request.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)
    try{Reject {Invoke-ControlRequest @requestArgs} 'agent_busy'}finally{$guard.Dispose()}
    $requestArgs.StateDirectory=Join-Path $temp 'publication-failure'
    $requestArgs.Publisher={throw 'simulated-network-failure'}
    Reject {Invoke-ControlRequest @requestArgs} 'simulated-network-failure'
    Assert ($script:executions -eq 2) 'operation finished before publication failure'
    $requestArgs.Publisher=$publisher;$result=Invoke-ControlRequest @requestArgs
    Assert ($script:executions -eq 2 -and $result.state -eq 'succeeded') 'publication retry never executes again'
    $requestArgs.StateDirectory=Join-Path $temp 'crash'
    Write-AtomicJson (Join-Path $requestArgs.StateDirectory ($id+'.json')) @{request_sha=$sha;published=$false;result=@{state='executing';code='running'}}
    $result=Invoke-ControlRequest @requestArgs
    Assert ($result.state -eq 'unknown' -and $result.code -eq 'interrupted_do_not_replay' -and $script:executions -eq 2) 'crash recovery does not replay'
    $requestArgs.StateDirectory=Join-Path $temp 'read-only';$request.operation='restart';$requestArgs.Json=Json
    $result=Invoke-ControlRequest @requestArgs
    Assert ($result.code -eq 'mutations_disabled' -and $script:executions -eq 2) 'read-only default'
    $requestArgs.StateDirectory=Join-Path $temp 'timeout';$requestArgs.AllowMutations=$true;$requestArgs.Executor={throw 'start_timeout_unknown'}
    $result=Invoke-ControlRequest @requestArgs
    Assert ($result.state -eq 'unknown') 'start timeout remains unknown'
    $requestArgs.StateDirectory=Join-Path $temp 'raw-error';$requestArgs.Executor={throw 'password=secret'}
    $result=Invoke-ControlRequest @requestArgs
    Assert ($result.code -eq 'local_control_failed' -and ($result | ConvertTo-Json) -notmatch 'password|secret') 'raw exception is not published'

    $empty='{}' | ConvertFrom-Json
    $plan=Get-TestHttpsPlan 'pc.tail123.ts.net.' $empty
    Assert (!$plan.already_configured -and $plan.public_origin -eq 'https://pc.tail123.ts.net') 'HTTPS plan from real DNS name'
    $serve='{"TCP":{"443":{"HTTPS":true}},"Web":{"pc.tail123.ts.net:443":{"Handlers":{"/":{"Proxy":"http://127.0.0.1:18740"}}}},"AllowFunnel":{"pc.tail123.ts.net:443":false}}' | ConvertFrom-Json
    Assert (Get-TestHttpsPlan 'pc.tail123.ts.net' $serve).already_configured 'HTTPS idempotence'
    Reject {Get-TestHttpsPlan 'evil.example' $empty} 'invalid_tailscale_dns'
    $serve.AllowFunnel.'pc.tail123.ts.net:443'=$true
    Reject {Get-TestHttpsPlan 'pc.tail123.ts.net' $serve} 'funnel_must_be_disabled'
    $serve.AllowFunnel.'pc.tail123.ts.net:443'=$false
    $serve.Web.'pc.tail123.ts.net:443'.Handlers.'/'.Proxy='http://127.0.0.1:8740'
    Reject {Get-TestHttpsPlan 'pc.tail123.ts.net' $serve} 'https_port_in_use'

    # Real git/filesystem checks for build provenance, without launching Java or MySQL.
    $buildRoot=Join-Path $temp 'build'
    [IO.Directory]::CreateDirectory($buildRoot) | Out-Null
    Push-Location $buildRoot
    try{
        & git init -q
        foreach($service in @('inventory','imaging')){
            [IO.Directory]::CreateDirectory((Join-Path $buildRoot ($service+'/src'))) | Out-Null
            [IO.Directory]::CreateDirectory((Join-Path $buildRoot ($service+'/target'))) | Out-Null
            [IO.File]::WriteAllText((Join-Path $buildRoot ($service+'/src/source.txt')),'source')
            [IO.File]::WriteAllText((Join-Path $buildRoot ($service+'/target/'+$service+'-1.0.0.jar')),'jar')
        }
        & git add inventory/src imaging/src
        & git -c user.name=Tests -c user.email=tests@example.invalid commit -qm fixture
        if($LASTEXITCODE -ne 0){throw 'test_git_fixture_failed'}
        $commit=(& git rev-parse HEAD | Out-String).Trim()
        $buildHash=(Get-FileHash inventory/target/inventory-1.0.0.jar).Hash.ToLowerInvariant()
        Write-AtomicJson (Join-Path $buildRoot '.local/build-info.json') @{source_commit=$commit;inventory_sha256=$buildHash;imaging_sha256=$buildHash}
        & (Get-Module TestServer.Core) {param($Root) Assert-TestBuild $Root} $buildRoot
        Assert $true 'clean committed build accepted'
        [IO.File]::WriteAllText((Join-Path $buildRoot 'inventory/target/inventory-1.0.0.jar'),'changed')
        Reject {& (Get-Module TestServer.Core) {param($Root) Assert-TestBuild $Root} $buildRoot} 'verified_build_required'
        [IO.File]::WriteAllText((Join-Path $buildRoot 'inventory/target/inventory-1.0.0.jar'),'jar')
        [IO.File]::WriteAllText((Join-Path $buildRoot 'inventory/src/source.txt'),'changed')
        Reject {& (Get-Module TestServer.Core) {param($Root) Assert-TestBuild $Root} $buildRoot} 'source_changed_since_build'
        [IO.File]::WriteAllText((Join-Path $buildRoot 'inventory/src/source.txt'),'source')
        [IO.File]::WriteAllText((Join-Path $buildRoot 'inventory/src/new.txt'),'untracked')
        Reject {& (Get-Module TestServer.Core) {param($Root) Assert-TestBuild $Root} $buildRoot} 'source_changed_since_build'
    }finally{Pop-Location}

    # Test server mocks cannot launch or stop a real process.
    $serverModule=Get-Module TestServer.Core
    & $serverModule {
        function script:Test-LocalHealth {param($Port) return $false}
        function script:Test-LocalDatabase {return $false}
        function script:Get-CimInstance {param($ClassName,$Filter) return [pscustomobject]@{Name='java.exe';CommandLine='unrelated.jar'}}
        function script:Stop-Process {throw 'TEST_MUST_NOT_STOP_PROCESS'}
    }
    $serverRoot=Join-Path $temp 'server';$acceptance=Join-Path $serverRoot '.local/acceptance'
    [IO.Directory]::CreateDirectory($acceptance) | Out-Null
    [IO.File]::WriteAllText((Join-Path $acceptance 'inventory.pid'),'12345')
    $personal=Join-Path $serverRoot '.local/personal/keep.txt';[IO.Directory]::CreateDirectory((Split-Path $personal)) | Out-Null;[IO.File]::WriteAllText($personal,'keep')
    Reject {Invoke-TestServer stop $serverRoot} 'process_ownership_mismatch'
    Assert ((Get-Content -LiteralPath $personal -Raw) -eq 'keep') 'personal profile unchanged'
    [IO.File]::WriteAllText((Join-Path $acceptance 'runtime.txt'),'docker')
    Reject {Invoke-TestServer start $serverRoot} 'native_test_profile_required'
    [IO.File]::WriteAllText((Join-Path $acceptance 'runtime.txt'),'native')
    Reject {Invoke-TestServer start $serverRoot} 'verified_build_required'
    Assert ((Invoke-TestServer status $serverRoot).environment -eq 'teszt') 'status uses isolated profile'

    Remove-Item -LiteralPath (Join-Path $acceptance 'inventory.pid') -Force
    [IO.File]::WriteAllText((Join-Path $acceptance 'mysql.pid'),'65432')
    foreach($service in @('inventory','imaging')){[IO.File]::WriteAllText((Join-Path $acceptance ($service+'.jar')),'mock-jar')}
    $hash=(Get-FileHash (Join-Path $acceptance 'inventory.jar')).Hash.ToLowerInvariant()
    Write-AtomicJson (Join-Path $serverRoot '.local/build-info.json') @{source_commit=('a'*40);inventory_sha256=$hash;imaging_sha256=$hash}
    & $serverModule {
        $script:testStarted=0;$script:testUp=$false;$script:testStopped=@()
        function script:Assert-TestBuild {param($Root)}
        function script:Test-LocalHealth {param($Port) return $script:testUp}
        function script:Invoke-TestStartProcess {
            param($Root)
            if($env:DB_HOST -ne '127.0.0.1' -or $env:BIND_ADDRESS -ne '127.0.0.1' -or $env:SESSION_COOKIE_SECURE -ne 'false'){throw 'UNSAFE_START_ENVIRONMENT'}
            $script:testStarted++;$script:testUp=$true
        }
    }
    $oldDbHost=$env:DB_HOST;$env:DB_HOST='must-restore'
    try{$started=Invoke-TestServer start $serverRoot;Assert ($env:DB_HOST -eq 'must-restore') 'start restores caller environment'}finally{$env:DB_HOST=$oldDbHost}
    Assert $started.inventory_up 'mock start waits for health'
    Invoke-TestServer start $serverRoot | Out-Null
    Assert ((& $serverModule {$script:testStarted}) -eq 1) 'idempotent start'
    [IO.File]::WriteAllText((Join-Path $acceptance 'imaging.jar'),'different-jar')
    Reject {Invoke-TestServer start $serverRoot} 'restart_required'
    foreach($service in @('inventory','imaging')){[IO.File]::WriteAllText((Join-Path $acceptance ($service+'.pid')),'12345')}
    & $serverModule {
        $script:testAcceptance=$null
        function script:Get-CimInstance {param($ClassName,$Filter) return [pscustomobject]@{Name='java.exe';CommandLine=('-Dotthontar.state="'+$script:testAcceptance+'" -jar "'+(Join-Path $script:testAcceptance 'inventory.jar')+'" "'+(Join-Path $script:testAcceptance 'imaging.jar')+'"')}}
        function script:Stop-Process {param($Id) $script:testStopped+=@($Id);$script:testUp=$false}
    }
    & $serverModule {param($Value) $script:testAcceptance=$Value} $acceptance
    Invoke-TestServer stop $serverRoot | Out-Null
    Assert ((& $serverModule {$script:testStopped.Count}) -eq 2) 'only two owned application processes stopped'
    Assert ((Get-Content -LiteralPath (Join-Path $acceptance 'mysql.pid') -Raw) -eq '65432') 'database process untouched'
    Assert ((Get-Content -LiteralPath $personal -Raw) -eq 'keep') 'personal data preserved after start and stop'
    Reject {Invoke-TestServer start $serverRoot} 'restart_required'


    # Exercise the full poll cycle with fake GitHub, including private-repo gate,
    # immutable commit reads and result write conflict recovery.
    $controlModule=Get-Module Control.Core
    & $controlModule {
        $script:testPrivate=$true;$script:testWrites=0;$script:testExecutes=0;$script:testConflict=$true
        $script:testId=[guid]::NewGuid().ToString()
        $script:testPayload=@{schema_version=1;request_id=$script:testId;server_id='windows-test';environment='teszt';operation='status';created_at=[DateTimeOffset]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ');expires_at=[DateTimeOffset]::UtcNow.AddMinutes(5).ToString('yyyy-MM-ddTHH:mm:ssZ')} | ConvertTo-Json -Compress
        function script:Invoke-ControlApi {
            param($Token,$Path,$Method='GET',$Body)
            if($Path -eq '/repos/example/private-ops'){return [pscustomobject]@{private=$script:testPrivate;full_name='example/private-ops'}}
            if($Path.EndsWith('/commits/main')){return [pscustomobject]@{sha=('a'*40)}}
            if($Path -match '/contents/requests\?ref=([a-f0-9]{40})$'){
                return [pscustomobject]@{type='file';name=($script:testId+'.json');path=('requests/'+$script:testId+'.json');size=400;sha=('c'*40)}
            }
            if($Path -match '/contents/requests/.+\?ref=([a-f0-9]{40})$'){return [pscustomobject]@{sha=('c'*40);encoding='base64';size=400;content=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($script:testPayload))}}
            if($Path -match '/contents/results/'){
                if($Method -eq 'GET'){throw 'github_not_found'}
                $script:testWrites++
                if($script:testConflict){$script:testConflict=$false;throw 'github_request_failed'}
                return @{}
            }
            throw 'UNEXPECTED_API_PATH'
        }
        function script:Invoke-TestServer {param($Action,$Root) $script:testExecutes++;return @{inventory_up=$true}}
    }
    $config=[pscustomobject]@{repository='example/private-ops';server_id='windows-test';branch='main';allow_mutations=$false}
    $cycleArgs=@{Config=$config;Token='fake';Root=$serverRoot;StateDirectory=(Join-Path $temp 'cycle')}
    Reject {Invoke-ControlCycle @cycleArgs} 'github_request_failed'
    Invoke-ControlCycle @cycleArgs
    Invoke-ControlCycle @cycleArgs
    $counts=& $controlModule {@($script:testExecutes,$script:testWrites)}
    Assert ($counts[0] -eq 1 -and $counts[1] -eq 2) 'GitHub conflict retries publication only'
    & $controlModule {$script:testPrivate=$false}
    Reject {Invoke-ControlCycle @cycleArgs} 'private_control_repository_required'
    Write-Host "PASS: $script:passed assertions (parser, protocol, journal, mock GitHub, HTTPS and test-profile guards)"
}finally{Remove-Item -LiteralPath $temp -Recurse -Force}

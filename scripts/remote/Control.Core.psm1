Set-StrictMode -Version 2
$ErrorActionPreference='Stop'
Import-Module (Join-Path $PSScriptRoot 'TestServer.Core.psm1') -Force

function Convert-ControlJson {
    param([string]$Json)
    # PowerShell 7.5 may otherwise turn ISO timestamp strings into DateTime values.
    if((Get-Command ConvertFrom-Json).Parameters.ContainsKey('DateKind')){return ConvertFrom-Json -InputObject $Json -DateKind String}
    return ConvertFrom-Json -InputObject $Json
}

function Get-ControlRequest {
    param([string]$Json,[string]$RequestId,[string]$ServerId,[DateTimeOffset]$Now=[DateTimeOffset]::UtcNow)
    if([Text.Encoding]::UTF8.GetByteCount($Json) -gt 4096 -or $RequestId -cnotmatch '^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$'){throw 'rejected_schema'}
    try{$request=Convert-ControlJson $Json}catch{throw 'rejected_schema'}
    if($null -eq $request -or $request -isnot [pscustomobject]){throw 'rejected_schema'}
    $expected=@('schema_version','request_id','server_id','environment','operation','created_at','expires_at')
    $properties=@($request.PSObject.Properties.Name)
    if($properties.Count -ne 7 -or @($properties | Where-Object {$_ -cnotin $expected}).Count){throw 'rejected_schema'}
    # Reject duplicate or nested keys rather than accepting last-property-wins JSON.
    foreach($key in $expected){if([regex]::Matches($Json,'(?<!\\)"'+$key+'"\s*:').Count -ne 1){throw 'rejected_schema'}}
    if(($request.schema_version -isnot [int] -and $request.schema_version -isnot [long]) -or $request.schema_version -ne 1){throw 'rejected_schema'}
    foreach($key in $expected | Where-Object {$_ -ne 'schema_version'}){if($request.$key -isnot [string]){throw 'rejected_schema'}}
    if($request.request_id -cne $RequestId -or $request.server_id -cne $ServerId -or $request.environment -cne 'teszt'){throw 'rejected_target'}
    if($request.operation -cnotin @('status','diagnostics','start','stop','restart')){throw 'rejected_action'}
    $created=[DateTimeOffset]::MinValue;$expires=[DateTimeOffset]::MinValue
    $pattern='^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{1,7})?Z$'
    if($request.created_at -cnotmatch $pattern -or $request.expires_at -cnotmatch $pattern -or
        ![DateTimeOffset]::TryParse($request.created_at,[Globalization.CultureInfo]::InvariantCulture,[Globalization.DateTimeStyles]::AssumeUniversal,[ref]$created) -or
        ![DateTimeOffset]::TryParse($request.expires_at,[Globalization.CultureInfo]::InvariantCulture,[Globalization.DateTimeStyles]::AssumeUniversal,[ref]$expires)){throw 'rejected_schema'}
    if($created -gt $Now.AddSeconds(30) -or $expires -le $Now -or $expires -le $created -or ($expires-$created).TotalSeconds -gt 600){throw 'rejected_expired'}
    return $request
}

function Select-ControlData {
    param($Data)
    # An allowlist, not regex redaction of raw logs. Never publish stdout, paths, headers or secrets.
    $clean=[ordered]@{environment='teszt'}
    foreach($key in @('inventory_up','imaging_up','database_tcp_open')){
        if($null -ne $Data -and $null -ne $Data.PSObject.Properties[$key] -and $Data.$key -is [bool]){$clean[$key]=$Data.$key}
        elseif($Data -is [Collections.IDictionary] -and $Data.Contains($key) -and $Data[$key] -is [bool]){$clean[$key]=$Data[$key]}
    }
    foreach($pair in @(@('source_commit','^[a-f0-9]{40}$'),@('inventory_sha256','^[a-f0-9]{64}$'),@('imaging_sha256','^[a-f0-9]{64}$'))){
        $key=$pair[0];$value=$null
        if($Data -is [Collections.IDictionary] -and $Data.Contains($key)){$value=$Data[$key]}
        elseif($null -ne $Data -and $null -ne $Data.PSObject.Properties[$key]){$value=$Data.$key}
        if($value -is [string] -and $value -cmatch $pair[1]){$clean[$key]=$value}
    }
    return $clean
}

function Invoke-ControlRequest {
    param([string]$Json,[string]$RequestId,[string]$RequestSha,[string]$ServerId,[string]$StateDirectory,
        [bool]$AllowMutations,[scriptblock]$Executor,[scriptblock]$Publisher,[DateTimeOffset]$Now=[DateTimeOffset]::UtcNow)
    if($RequestId -cnotmatch '^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$' -or $RequestSha -cnotmatch '^[a-f0-9]{40}$'){throw 'rejected_schema'}
    [IO.Directory]::CreateDirectory($StateDirectory) | Out-Null
    $guard=$null
    try{
        try{$guard=[IO.File]::Open((Join-Path $StateDirectory 'request.lock'),[IO.FileMode]::OpenOrCreate,[IO.FileAccess]::ReadWrite,[IO.FileShare]::None)}catch{throw 'agent_busy'}
        $path=Join-Path $StateDirectory ($RequestId+'.json')
        if(Test-Path -LiteralPath $path){
            $record=Convert-ControlJson (Get-Content -LiteralPath $path -Raw)
            if($record.request_sha -cne $RequestSha){throw 'request_changed'}
            if($record.result.state -eq 'executing'){
                $record.result.state='unknown';$record.result.code='interrupted_do_not_replay';$record.published=$false
                Write-AtomicJson $path $record
            }
            if(!$record.published){
                & $Publisher $RequestId $record.result | Out-Null
                $record.published=$true;Write-AtomicJson $path $record
            }
            return $record.result
        }
        $result=[ordered]@{schema_version=1;request_id=$RequestId;server_id=$ServerId;environment='teszt';operation=$null;state='rejected';code='rejected_schema';completed_at=$Now.ToString('yyyy-MM-ddTHH:mm:ssZ');data=@{}}
        try{
            $request=Get-ControlRequest -Json $Json -RequestId $RequestId -ServerId $ServerId -Now $Now
            $result.operation=$request.operation
            if(!$AllowMutations -and $request.operation -in @('start','stop','restart')){throw 'mutations_disabled'}
            $result.state='executing';$result.code='running'
        }catch{
            $allowed=@('rejected_schema','rejected_target','rejected_action','rejected_expired','mutations_disabled')
            $result.code=if($_.Exception.Message -in $allowed){$_.Exception.Message}else{'rejected_schema'}
        }
        $record=@{request_sha=$RequestSha;published=$false;result=$result}
        Write-AtomicJson $path $record
        if($result.state -eq 'executing'){
            try{
                $data=& $Executor $request.operation
                $result.data=Select-ControlData $data
                $result.state='succeeded';$result.code='ok'
            }catch{
                $result.state='failed';$result.code='local_control_failed'
                $known=@('verified_build_required','source_changed_since_build','native_test_profile_required','invalid_test_pid','process_ownership_mismatch','control_busy','restart_required','start_failed','health_failed','stop_not_confirmed','invalid_https_configuration','previous_start_running')
                if($_.Exception.Message -in $known){$result.code=$_.Exception.Message}
                if($_.Exception.Message -eq 'start_timeout_unknown'){$result.state='unknown';$result.code='start_timeout_unknown'}
            }
            $result.completed_at=[DateTimeOffset]::UtcNow.ToString('yyyy-MM-ddTHH:mm:ssZ')
            Write-AtomicJson $path $record
        }
        & $Publisher $RequestId $result | Out-Null
        $record.published=$true;Write-AtomicJson $path $record
        return $result
    }finally{if($null -ne $guard){$guard.Dispose()}}
}

function Invoke-ControlApi {
    param([string]$Token,[string]$Path,[ValidateSet('GET','PUT')][string]$Method='GET',$Body=$null)
    if($Path -notmatch '^/repos/[a-zA-Z0-9-]+/[a-zA-Z0-9._-]+(/|$)' -or $Path.Contains('..')){throw 'invalid_api_path'}
    $headers=@{Authorization='Bearer '+$Token;Accept='application/vnd.github+json';'User-Agent'='Otthontar-Restricted-Control/1';'X-GitHub-Api-Version'='2022-11-28'}
    $parameters=@{Uri='https://api.github.com'+$Path;Method=$Method;Headers=$headers;TimeoutSec=15;MaximumRedirection=0;UseBasicParsing=$true;ErrorAction='Stop'}
    if($null -ne $Body){$parameters.ContentType='application/json';$parameters.Body=[Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Depth 12 -Compress))}
    try{
        $response=Invoke-WebRequest @parameters
        if([Text.Encoding]::UTF8.GetByteCount($response.Content) -gt 1048576){throw 'api_response_too_large'}
        return Convert-ControlJson $response.Content
    }catch{
        if($null -ne $_.Exception.PSObject.Properties['Response'] -and $null -ne $_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 404){throw 'github_not_found'}
        throw 'github_request_failed'
    }
}

function Invoke-ControlCycle {
    param($Config,[string]$Token,[string]$Root,[string]$StateDirectory)
    $repository=[string]$Config.repository
    if($repository -cnotmatch '^[a-zA-Z0-9][a-zA-Z0-9-]{0,38}/[a-zA-Z0-9][a-zA-Z0-9._-]{0,99}$' -or $repository.Contains('..') -or
        $Config.server_id -cnotmatch '^[a-z0-9][a-z0-9-]{2,63}$' -or $Config.branch -cne 'main' -or $Config.allow_mutations -isnot [bool]){throw 'invalid_agent_configuration'}
    $base='/repos/'+$repository
    $metadata=Invoke-ControlApi -Token $Token -Path $base
    if($metadata.private -ne $true -or $metadata.full_name -cne $repository){throw 'private_control_repository_required'}
    $head=Invoke-ControlApi -Token $Token -Path ($base+'/commits/main')
    if($head.sha -cnotmatch '^[a-f0-9]{40}$'){throw 'invalid_github_response'}
    try{$items=@(Invoke-ControlApi -Token $Token -Path ($base+'/contents/requests?ref='+$head.sha))}catch{
        if($_.Exception.Message -eq 'github_not_found'){return};throw
    }
    if($items.Count -gt 100){throw 'queue_maintenance_required'}
    $processed=0
    foreach($item in ($items | Sort-Object name)){
        if($item.type -ne 'file' -or $item.name -cnotmatch '^([a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12})\.json$'){continue}
        $id=$Matches[1]
        if($item.path -cne ('requests/'+$id+'.json') -or $item.size -gt 4096 -or $item.sha -cnotmatch '^[a-f0-9]{40}$'){continue}
        $local=Join-Path $StateDirectory ($id+'.json')
        if(Test-Path -LiteralPath $local){
            $saved=Convert-ControlJson (Get-Content -LiteralPath $local -Raw)
            if($saved.request_sha -cne $item.sha){throw 'request_changed'}
            if($saved.published -and $saved.result.state -ne 'executing'){continue}
        }
        if($processed -ge 5){break}
        $file=Invoke-ControlApi -Token $Token -Path ($base+'/contents/requests/'+$id+'.json?ref='+$head.sha)
        if($file.sha -cne $item.sha -or $file.encoding -ne 'base64' -or $file.size -gt 4096){throw 'invalid_github_response'}
        $bytes=[Convert]::FromBase64String($file.content)
        if($bytes.Length -gt 4096){throw 'invalid_github_response'}
        $json=[Text.UTF8Encoding]::new($false,$true).GetString($bytes)
        $publisher={param($requestId,$result)
            $path=$base+'/contents/results/'+$requestId+'.json'
            $body=@{message='control result '+$requestId;branch='main';content=[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes(($result | ConvertTo-Json -Depth 12 -Compress)))}
            try{
                $existing=Invoke-ControlApi -Token $Token -Path ($path+'?ref=main')
                if($existing.sha -cnotmatch '^[a-f0-9]{40}$'){throw 'invalid_github_response'}
                $body.sha=$existing.sha
            }catch{if($_.Exception.Message -ne 'github_not_found'){throw}}
            # A conflict is retried on the next cycle using a fresh SHA; never rerun the operation.
            Invoke-ControlApi -Token $Token -Path $path -Method PUT -Body $body | Out-Null
        }
        $executor={param($action) Invoke-TestServer -Action $action -Root $Root}
        Invoke-ControlRequest -Json $json -RequestId $id -RequestSha $item.sha -ServerId $Config.server_id -StateDirectory $StateDirectory -AllowMutations $Config.allow_mutations -Executor $executor -Publisher $publisher | Out-Null
        $processed++
    }
}

Export-ModuleMember -Function Convert-ControlJson,Get-ControlRequest,Select-ControlData,Invoke-ControlRequest,Invoke-ControlCycle

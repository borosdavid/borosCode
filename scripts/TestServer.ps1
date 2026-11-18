param([ValidateSet('status','diagnostics','start','stop','restart')][string]$Action='status')
$ErrorActionPreference='Stop'
if($env:OS -ne 'Windows_NT'){throw 'Ez a belépési pont a Windows tesztprofilhoz készült.'}
Import-Module (Join-Path $PSScriptRoot 'remote/TestServer.Core.psm1') -Force
$root=Split-Path $PSScriptRoot
try{
    Invoke-TestServer -Action $Action -Root $root | ConvertTo-Json -Depth 5
}catch{
    # The local start.log/start.err contains diagnostics. Do not echo DB secrets or raw child output.
    $known=@('verified_build_required','source_changed_since_build','native_test_profile_required','invalid_test_pid','process_ownership_mismatch','control_busy','restart_required','start_timeout_unknown','start_failed','health_failed','stop_not_confirmed','invalid_https_configuration','previous_start_running')
    $code=if($_.Exception.Message -in $known){$_.Exception.Message}else{'local_control_failed'}
    Write-Error $code
    exit 1
}

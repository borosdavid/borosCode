param([switch]$Once)
$ErrorActionPreference='Stop'
if($env:OS -ne 'Windows_NT'){throw 'Windows DPAPI required.'}
$principal=[Security.Principal.WindowsPrincipal]::new([Security.Principal.WindowsIdentity]::GetCurrent())
if($principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)){throw 'Run the controller as your normal Windows user, without elevation.'}
$root=Split-Path $PSScriptRoot
$directory=Join-Path $root '.local/remote-control'
Import-Module (Join-Path $directory 'modules/Control.Core.psm1') -Force
do{
    $pointer=[IntPtr]::Zero;$token=$null;$secret=$null
    try{
        $config=Get-Content -LiteralPath (Join-Path $directory 'config.json') -Raw | ConvertFrom-Json
        $secret=Get-Content -LiteralPath (Join-Path $directory 'token.dpapi') -Raw | ConvertTo-SecureString
        $pointer=[Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
        $token=[Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
        Invoke-ControlCycle -Config $config -Token $token -Root $root -StateDirectory (Join-Path $directory 'journal')
        Write-Host 'control_cycle_ok'
    }catch{
        $known=@('private_control_repository_required','invalid_agent_configuration','request_changed','agent_busy','queue_maintenance_required','github_request_failed','invalid_github_response')
        $code=if($_.Exception.Message -in $known){$_.Exception.Message}else{'control_cycle_failed'}
        Write-Host $code
        if($Once){exit 1}
    }finally{
        $token=$null
        if($pointer -ne [IntPtr]::Zero){[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)}
        if($null -ne $secret){$secret.Dispose()}
    }
    if(!$Once){Start-Sleep -Seconds 30}
}while(!$Once)

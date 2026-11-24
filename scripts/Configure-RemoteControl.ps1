param(
    [Parameter(Mandatory=$true)][ValidatePattern('^[a-zA-Z0-9][a-zA-Z0-9-]{0,38}/[a-zA-Z0-9][a-zA-Z0-9._-]{0,99}$')][string]$Repository,
    [ValidatePattern('^[a-z0-9][a-z0-9-]{2,63}$')][string]$ServerId='windows-test',
    [switch]$EnableMutations,
    [switch]$ReplaceToken
)
$ErrorActionPreference='Stop'
if($env:OS -ne 'Windows_NT'){throw 'Windows DPAPI required.'}
if($Repository.Contains('..')){throw 'Invalid repository.'}
Import-Module (Join-Path $PSScriptRoot 'remote/TestServer.Core.psm1') -Force
$root=Split-Path $PSScriptRoot
$directory=Join-Path $root '.local/remote-control'
[IO.Directory]::CreateDirectory($directory) | Out-Null
# Restrict local secrets and journals to the Windows user. No token argument or plaintext file.
$identity=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
& icacls.exe $directory /inheritance:r /grant:r ('*'+$identity+':(OI)(CI)F') | Out-Null
if($LASTEXITCODE -ne 0){throw 'Cannot protect local controller directory.'}
$tokenPath=Join-Path $directory 'token.dpapi'
if($ReplaceToken -or !(Test-Path -LiteralPath $tokenPath)){
    $secret=Read-Host 'Fine-grained GitHub token for the private control repository (Contents read/write only)' -AsSecureString
    try{
        if($secret.Length -eq 0){throw 'Empty token.'}
        $encrypted=ConvertFrom-SecureString $secret
        [IO.File]::WriteAllText($tokenPath,$encrypted,[Text.UTF8Encoding]::new($false))
    }finally{$secret.Dispose()}
}
# Install a reviewed local copy; requests never fetch scripts or application code.
$installed=Join-Path $directory 'modules'
[IO.Directory]::CreateDirectory($installed) | Out-Null
foreach($module in @('Control.Core.psm1','TestServer.Core.psm1')){
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot ('remote/'+$module)) -Destination (Join-Path $installed $module) -Force
}
Write-AtomicJson (Join-Path $directory 'config.json') @{repository=$Repository;server_id=$ServerId;branch='main';allow_mutations=[bool]$EnableMutations}
Write-Host 'Configured. First run scripts/Run-RemoteControl.ps1 -Once; use a private repository. No requests were executed by this setup.'

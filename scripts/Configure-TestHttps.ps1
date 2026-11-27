param([switch]$Enable)
$ErrorActionPreference='Stop'
if($env:OS -ne 'Windows_NT'){throw 'Windows required.'}
Import-Module (Join-Path $PSScriptRoot 'remote/Https.Core.psm1') -Force
Import-Module (Join-Path $PSScriptRoot 'remote/TestServer.Core.psm1') -Force
$tailscale=(Get-Command tailscale.exe -ErrorAction Stop).Source
$statusText=& $tailscale status --json
if($LASTEXITCODE -ne 0){throw 'Tailscale status failed.'}
$status=$statusText | Out-String | ConvertFrom-Json
if($status.BackendState -ne 'Running'){throw 'Sign in to Tailscale on this PC first.'}
$serveText=& $tailscale serve status --json
if($LASTEXITCODE -ne 0){throw 'Tailscale Serve status failed.'}
$serve=$serveText | Out-String | ConvertFrom-Json
$plan=Get-TestHttpsPlan -DnsName $status.Self.DNSName -ServeConfig $serve
if(!$Enable){$plan | ConvertTo-Json;Write-Host 'Use -Enable in an Administrator terminal to apply.';exit 0}
if(!$plan.already_configured){
    & $tailscale serve --bg --https=443 http://127.0.0.1:18740
    if($LASTEXITCODE -ne 0){throw 'Tailscale Serve failed. Enable HTTPS in the tailnet if requested, then retry.'}
}
$afterText=& $tailscale serve status --json
if($LASTEXITCODE -ne 0){throw 'Serve verification failed.'}
$after=Get-TestHttpsPlan -DnsName $status.Self.DNSName -ServeConfig ($afterText | Out-String | ConvertFrom-Json)
if(!$after.already_configured){throw 'Serve verification failed.'}
$root=Split-Path $PSScriptRoot
Write-AtomicJson (Join-Path $root '.local/remote-test/https.json') @{public_origin=$after.public_origin}
Write-Host ('Configured: '+$after.public_origin)
Write-Host 'Restart the test application using scripts/TestServer.ps1 -Action restart. Test login from iPhone with Tailscale enabled.'

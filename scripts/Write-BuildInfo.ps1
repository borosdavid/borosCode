$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
Push-Location $root
try{
    $source=(& git rev-parse HEAD | Out-String).Trim()
    if($LASTEXITCODE -ne 0 -or $source -cnotmatch '^[a-f0-9]{40}$'){throw 'Git commit required.'}
    $dirty=@(& git status --porcelain -- pom.xml inventory/src inventory/pom.xml imaging/src imaging/pom.xml)
    if($LASTEXITCODE -ne 0 -or $dirty.Count -gt 0){throw 'Commit the application sources before creating build provenance.'}
    $info=@{source_commit=$source}
    foreach($service in @('inventory','imaging')){
        $info[$service+'_sha256']=(Get-FileHash -LiteralPath "$service/target/$service-1.0.0.jar" -Algorithm SHA256).Hash.ToLowerInvariant()
    }
    Import-Module (Join-Path $PSScriptRoot 'remote/TestServer.Core.psm1') -Force
    Write-AtomicJson (Join-Path $root '.local/build-info.json') $info
}finally{Pop-Location}

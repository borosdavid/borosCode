param(
    [Parameter(Mandatory=$true)][ValidateSet('create','password','disable','enable')][string]$Action,
    [Parameter(Mandatory=$true)][string]$UserName,
    [switch]$TestRun
)
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot)
$profileName=if($TestRun){'acceptance'}else{'personal'}
$secretFile=Join-Path (Get-Location) ".local/$profileName/access.json"
if(!(Test-Path $secretFile)){throw 'Előbb készítsd elő a megfelelő helyi profilt az indítóval. A fiókeszköz nem indít vagy inicializál adatbázist.'}
$java=(Get-Command java.exe -ErrorAction SilentlyContinue).Source
if(Test-Path 'C:/Program Files/Java/jdk-21/bin/java.exe'){$java='C:/Program Files/Java/jdk-21/bin/java.exe'}
if(Test-Path '.local/jdk-25.0.5/bin/java.exe'){$java=(Resolve-Path '.local/jdk-25.0.5/bin/java.exe').Path}
if(!$java -or !(Test-Path inventory/target/inventory-1.0.0.jar)){throw 'Java és lefordított inventory JAR szükséges.'}
$keys=Get-Content -Raw $secretFile | ConvertFrom-Json
$old=@{}
foreach($key in @('DB_HOST','DB_PORT','DB_PASSWORD')){$old[$key]=[Environment]::GetEnvironmentVariable($key,'Process')}
try{
    $env:DB_HOST='127.0.0.1'
    $env:DB_PORT=if($TestRun){'43316'}else{'33316'}
    $env:DB_PASSWORD=$keys.app
    & $java -jar inventory/target/inventory-1.0.0.jar account $Action $UserName
    if($LASTEXITCODE){throw 'A fiókművelet nem sikerült. Ellenőrizd a helyi profil elérhetőségét.'}
}finally{
    foreach($key in $old.Keys){[Environment]::SetEnvironmentVariable($key,$old[$key],'Process')}
}

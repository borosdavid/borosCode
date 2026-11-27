$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot)
$maven=if(Test-Path .local/apache-maven-3.10.0/bin/mvn.cmd){(Resolve-Path .local/apache-maven-3.10.0/bin/mvn.cmd).Path}else{(Get-Command mvn.cmd -ErrorAction Stop).Source}
if(Test-Path .local/jdk-25.0.5/bin/javac.exe){$env:JAVA_HOME=(Resolve-Path .local/jdk-25.0.5).Path}
$arguments=@('-B','-ntp','-Dmaven.repo.local=.local/m2')
if(Test-Path .local/settings.xml){$arguments+=@('-s','.local/settings.xml')}
& $maven @arguments verify
if($LASTEXITCODE){exit $LASTEXITCODE}
& (Join-Path $PSScriptRoot 'Write-BuildInfo.ps1')
exit 0

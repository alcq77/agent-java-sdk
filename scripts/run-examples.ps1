Param(
  [string]$Root = "d:\giteePro\aiAgent"
)

$ErrorActionPreference = "Stop"

Write-Host "[1/3] Install local modules..."
Set-Location "$Root\\cq-agent-parent"
mvn -q clean install -DskipTests

Write-Host "[2/3] Run java-sdk demo..."
Set-Location "$Root\examples\java-sdk-demo"
mvn -q compile exec:java -Dexec.mainClass=demo.Main

Write-Host "[3/3] Run spring-starter demo..."
Set-Location "$Root\examples\spring-starter-demo"
mvn spring-boot:run

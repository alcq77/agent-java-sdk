Param(
  [string]$Root = "d:\giteePro\aiAgent"
)

$ErrorActionPreference = "Stop"

Write-Host "[preflight] compile..."
Set-Location "$Root\\cq-agent-parent"
mvn -q clean compile -DskipTests

Write-Host "[preflight] unit tests..."
mvn -q test

Write-Host "[preflight] done."

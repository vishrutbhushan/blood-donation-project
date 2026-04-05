$ErrorActionPreference = "Stop"
$basePath = "src\main\java\com\G9\hemoconnect"
New-Item -ItemType Directory -Force -Path "$basePath\entity" | Out-Null
New-Item -ItemType Directory -Force -Path "$basePath\repository" | Out-Null
Write-Output "Directories created successfully"

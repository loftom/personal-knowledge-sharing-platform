$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker command not found. Please install and start Docker Desktop first.'
}

docker compose up --build -d
if ($LASTEXITCODE -ne 0) {
    throw 'docker compose up failed. Make sure Docker Desktop is running and the engine is available.'
}

Write-Host ''
Write-Host 'Docker startup finished. Available endpoints:'
Write-Host 'Frontend: http://localhost:5173'
Write-Host 'Backend: http://localhost:8080'
Write-Host ''
Write-Host 'Status: docker compose ps'
Write-Host 'Logs: docker compose logs -f'

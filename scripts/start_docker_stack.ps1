param(
    [switch]$Build
)

$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker command not found. Please install and start Docker Desktop first.'
}

$composeArgs = @('compose', 'up', '-d')
if ($Build) {
    $composeArgs = @('compose', 'up', '--build', '-d')
}

& docker @composeArgs
if ($LASTEXITCODE -ne 0) {
    throw 'docker compose up failed. Make sure Docker Desktop is running and the engine is available.'
}

Write-Host ''
Write-Host 'Docker startup finished. Available endpoints:'
Write-Host 'Frontend: http://localhost:5173'
Write-Host 'Backend: http://localhost:8080'
Write-Host ''
if ($Build) {
    Write-Host 'Mode: rebuild images and start containers'
} else {
    Write-Host 'Mode: start existing containers without rebuild'
    Write-Host "Tip: use '.\scripts\start_docker_stack.ps1 -Build' after code or Dockerfile changes"
}
Write-Host ''
Write-Host 'Status: docker compose ps'
Write-Host 'Logs: docker compose logs -f'

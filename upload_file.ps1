param(
  [string]$token
)
$root = $PSScriptRoot
$filePath = Join-Path $root 'test_upload.txt'
$responsePath = Join-Path $root 'upload_response.json'

$res = Invoke-RestMethod -Uri 'http://localhost:8080/api/uploads' -Method Post -Form @{ file = Get-Item $filePath } -Headers @{ Authorization = "Bearer $token" }
$res | ConvertTo-Json -Compress | Out-File -FilePath $responsePath -Encoding utf8
Write-Output "DONE"


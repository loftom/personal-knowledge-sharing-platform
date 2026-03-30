param(
  [string]$token
)
$res = Invoke-RestMethod -Uri 'http://localhost:8080/api/uploads' -Method Post -Form @{ file = Get-Item 'd:\code\java\personal-knowledge-sharing-platform\test_upload.txt' } -Headers @{ Authorization = "Bearer $token" }
$res | ConvertTo-Json -Compress | Out-File -FilePath d:\code\java\personal-knowledge-sharing-platform\upload_response.json -Encoding utf8
Write-Output "DONE"


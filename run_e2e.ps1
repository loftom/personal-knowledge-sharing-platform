try {
  $root = $PSScriptRoot
  $loginPayloadPath = Join-Path $root 'login_payload.json'
  $uploadSourcePath = Join-Path $root 'test_upload.txt'
  $uploadResponsePath = Join-Path $root 'upload_response.json'
  $draftPayloadPath = Join-Path $root 'draft_payload.json'
  $publishPayloadPath = Join-Path $root 'publish_payload.json'

  $login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body (Get-Content $loginPayloadPath -Raw) -ContentType 'application/json' -ErrorAction Stop
  $token = $login.data.token
  Write-Output "TOKEN:$token"

  $curlArgs = @('-s', '-H', "Authorization: Bearer $token", '-F', "file=@$uploadSourcePath", 'http://localhost:8080/api/uploads', '-o', $uploadResponsePath)
  Start-Process -FilePath 'curl.exe' -ArgumentList $curlArgs -NoNewWindow -Wait
  if (Test-Path $uploadResponsePath) { Get-Content $uploadResponsePath -Raw | Write-Output } else { Write-Output 'NO_UPLOAD_RESPONSE' }

  # Use Invoke-RestMethod for JSON POST to /api/content
  try {
    $draftBody = Get-Content $draftPayloadPath -Raw
    $draftRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/content' -Method Post -Body $draftBody -ContentType 'application/json' -Headers @{ Authorization = "Bearer $token" }
    $draftRes | ConvertTo-Json -Compress | Write-Output
  } catch {
    Write-Output "NO_DRAFT_RESPONSE: $($_.Exception.Message)"
  }

  try {
    $pubBody = Get-Content $publishPayloadPath -Raw
    $pubRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/content' -Method Post -Body $pubBody -ContentType 'application/json' -Headers @{ Authorization = "Bearer $token" }
    $pubRes | ConvertTo-Json -Compress | Write-Output
  } catch {
    Write-Output "NO_PUBLISH_RESPONSE: $($_.Exception.Message)"
  }

} catch {
  Write-Output "E2E_ERROR: $($_.Exception.Message)"
}



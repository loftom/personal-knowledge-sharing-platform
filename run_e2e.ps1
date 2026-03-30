try {
  $login = Invoke-RestMethod -Uri 'http://localhost:8080/api/auth/login' -Method Post -Body (Get-Content 'd:\code\java\personal-knowledge-sharing-platform\login_payload.json' -Raw) -ContentType 'application/json' -ErrorAction Stop
  $token = $login.data.token
  Write-Output "TOKEN:$token"

  $curlArgs = @('-s','-H',"Authorization: Bearer $token",'-F',"file=@d:\\code\\java\\personal-knowledge-sharing-platform\\test_upload.txt",'http://localhost:8080/api/uploads','-o','d:\\code\\java\\personal-knowledge-sharing-platform\\upload_response.json')
  Start-Process -FilePath 'curl.exe' -ArgumentList $curlArgs -NoNewWindow -Wait
  if (Test-Path 'd:\code\java\personal-knowledge-sharing-platform\upload_response.json') { Get-Content 'd:\code\java\personal-knowledge-sharing-platform\upload_response.json' -Raw | Write-Output } else { Write-Output 'NO_UPLOAD_RESPONSE' }

  # Use Invoke-RestMethod for JSON POST to /api/content
  try {
    $draftBody = Get-Content 'd:\\code\\java\\personal-knowledge-sharing-platform\\draft_payload.json' -Raw
    $draftRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/content' -Method Post -Body $draftBody -ContentType 'application/json' -Headers @{ Authorization = "Bearer $token" }
    $draftRes | ConvertTo-Json -Compress | Write-Output
  } catch {
    Write-Output "NO_DRAFT_RESPONSE: $($_.Exception.Message)"
  }

  try {
    $pubBody = Get-Content 'd:\\code\\java\\personal-knowledge-sharing-platform\\publish_payload.json' -Raw
    $pubRes = Invoke-RestMethod -Uri 'http://localhost:8080/api/content' -Method Post -Body $pubBody -ContentType 'application/json' -Headers @{ Authorization = "Bearer $token" }
    $pubRes | ConvertTo-Json -Compress | Write-Output
  } catch {
    Write-Output "NO_PUBLISH_RESPONSE: $($_.Exception.Message)"
  }

} catch {
  Write-Output "E2E_ERROR: $($_.Exception.Message)"
}



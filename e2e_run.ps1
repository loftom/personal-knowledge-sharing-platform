$base='http://localhost:8080/api'
Write-Output "E2E START: $(Get-Date -Format o)"

# Register (ignore duplicate errors)
try {
  $reg = Invoke-RestMethod -Uri "$base/auth/register" -Method POST -Body (ConvertTo-Json @{username='e2e_user_run_uniq'; password='Pass123!'; nickname='E2E Run'}) -ContentType 'application/json' -ErrorAction Stop;
  Write-Output "REGISTER RESPONSE: $($reg | ConvertTo-Json -Depth 5)";
} catch {
  Write-Output "REGISTER ERROR (ok to continue): $($_.Exception.Message)";
  if ($_.Exception.Response) { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); Write-Output $sr.ReadToEnd() }
}

# Login
$token = $null
try {
  $login = Invoke-RestMethod -Uri "$base/auth/login" -Method POST -Body (ConvertTo-Json @{username='e2e_user_run_uniq'; password='Pass123!'}) -ContentType 'application/json' -ErrorAction Stop;
  Write-Output "LOGIN RESPONSE: $($login | ConvertTo-Json -Depth 5)";
  $token = $login.data.token
  Write-Output "TOKEN length: $($token.Length)";
} catch {
  Write-Output "LOGIN ERROR: $($_.Exception.Message)";
  if ($_.Exception.Response) { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); Write-Output $sr.ReadToEnd() }
}

if (-not $token) { Write-Output 'No token obtained — aborting E2E.'; exit 1 }

# Create content
$created = $null
try {
  $create = @{type='ARTICLE'; title='E2E Run Article Unique'; summary='e2e summary'; body='E2E run body content'; categoryId=1; visibility='PUBLIC'; tagIds=@(1)}
  $created = Invoke-RestMethod -Uri "$base/content" -Method POST -Body (ConvertTo-Json $create) -ContentType 'application/json' -Headers @{Authorization="Bearer $token"} -ErrorAction Stop;
  Write-Output "CREATE RESPONSE: $($created | ConvertTo-Json -Depth 5)";
} catch {
  Write-Output "CREATE ERROR: $($_.Exception.Message)";
  if ($_.Exception.Response) { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); Write-Output $sr.ReadToEnd() }
}

if ($created -eq $null) { Write-Output 'Create failed — aborting.'; exit 1 }
$cid = $created.data
Write-Output "Created content id: $cid"

# Search
try {
  $search = Invoke-RestMethod -Uri "$base/public/search?keyword=E2E" -Method GET -ErrorAction Stop;
  Write-Output "SEARCH RESPONSE: $($search | ConvertTo-Json -Depth 5)";
} catch {
  Write-Output "SEARCH ERROR: $($_.Exception.Message)";
}

# Comment
try {
  $comment = Invoke-RestMethod -Uri "$base/interaction/comment/$cid" -Method POST -Body (ConvertTo-Json @{body='Nice article from E2E run unique'}) -ContentType 'application/json' -Headers @{Authorization="Bearer $token"} -ErrorAction Stop;
  Write-Output "COMMENT RESPONSE: $($comment | ConvertTo-Json -Depth 5)";
} catch {
  Write-Output "COMMENT ERROR: $($_.Exception.Message)";
  if ($_.Exception.Response) { $sr = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream()); Write-Output $sr.ReadToEnd() }
}

# List comments
try {
  $comments = Invoke-RestMethod -Uri "$base/interaction/comment/$cid" -Method GET -ErrorAction Stop;
  Write-Output "COMMENTS LIST: $($comments | ConvertTo-Json -Depth 5)";
} catch {
  Write-Output "COMMENTS LIST ERROR: $($_.Exception.Message)";
}

Write-Output "E2E END: $(Get-Date -Format o)"

$uri = "http://localhost:8080/api/v1/positions/1"
$headers = @{
    "Content-Type" = "application/json"
    "Authorization" = "Basic " + [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin@example.com:admin"))
}

try {
    $response = Invoke-RestMethod -Uri $uri -Method GET -Headers $headers
    Write-Host "Response received successfully!"
    Write-Host "Position ID: $($response.id)"
    Write-Host "Title: $($response.title)"
    Write-Host "Company: $($response.company)"
    Write-Host "Topics: $($response.topics -join ', ')"
    Write-Host "Team members: $($response.team.Count)"
    if ($response.team -and $response.team.Count -gt 0) {
        foreach ($member in $response.team) {
            Write-Host "  - $($member.firstName) $($member.lastName) ($($member.email))"
        }
    }
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host "Status Code: $($_.Exception.Response.StatusCode)"
} 
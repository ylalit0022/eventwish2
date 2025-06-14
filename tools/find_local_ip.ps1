# PowerShell script to find local IP address for API server configuration

Write-Host "Finding local IP addresses for API server configuration..." -ForegroundColor Cyan
Write-Host "------------------------------------------------------------" -ForegroundColor Cyan

# Get IP addresses (excluding loopback)
$ipAddresses = Get-NetIPAddress | Where-Object { 
    $_.AddressFamily -eq "IPv4" -and 
    $_.IPAddress -ne "127.0.0.1" -and
    $_.IPAddress -notlike "169.254.*" # Exclude link-local addresses
}

Write-Host "Available IPv4 addresses:" -ForegroundColor Yellow
foreach ($ip in $ipAddresses) {
    Write-Host "$($ip.InterfaceAlias): $($ip.IPAddress)" -ForegroundColor Green
}

Write-Host "`nMost likely your local network IP address is:" -ForegroundColor Yellow
$mostLikelyIP = $ipAddresses | Where-Object { 
    $_.IPAddress -like "192.168.*" -or 
    $_.IPAddress -like "10.*" -or 
    $_.IPAddress -like "172.16.*" 
} | Select-Object -First 1 -ExpandProperty IPAddress

if ($mostLikelyIP) {
    Write-Host $mostLikelyIP -ForegroundColor Green
} else {
    Write-Host "Could not determine most likely IP. Please select from the list above." -ForegroundColor Red
}

Write-Host "`n------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "Use this IP address in your build.gradle file:" -ForegroundColor Yellow
Write-Host "buildConfigField `"String`", `"BASE_URL`", `"\`"http://$($mostLikelyIP):5000/api/\`"`"" -ForegroundColor White

Write-Host "`nAnd in your network_security_config.xml:" -ForegroundColor Yellow
Write-Host "<domain includeSubdomains=`"true`">$($mostLikelyIP)</domain>" -ForegroundColor White
Write-Host "------------------------------------------------------------" -ForegroundColor Cyan 
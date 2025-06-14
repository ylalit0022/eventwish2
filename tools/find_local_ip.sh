#!/bin/bash
# Script to find local IP address for API server configuration

echo "Finding local IP addresses for API server configuration..."
echo "------------------------------------------------------------"

# For Linux/macOS
if command -v ifconfig &> /dev/null; then
    echo "Network interfaces (ifconfig):"
    ifconfig | grep "inet " | grep -v 127.0.0.1 | awk '{print $2}'
    echo ""
fi

# For Windows (when run in Git Bash or similar)
if command -v ipconfig &> /dev/null; then
    echo "Network interfaces (ipconfig):"
    ipconfig | grep -A 2 "IPv4 Address" | grep -v 127.0.0.1
    echo ""
fi

# For both platforms with hostname
echo "Hostname method:"
hostname -I 2>/dev/null || echo "hostname -I command not available"
echo ""

echo "------------------------------------------------------------"
echo "Use one of these IP addresses in your build.gradle file:"
echo "buildConfigField \"String\", \"BASE_URL\", \"\\\"http://YOUR_IP_ADDRESS:5000/api/\\\"\""
echo ""
echo "And in your network_security_config.xml:"
echo "<domain includeSubdomains=\"true\">YOUR_IP_ADDRESS</domain>"
echo "------------------------------------------------------------" 
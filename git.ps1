# Git helper script for PowerShell
# Usage: .\git.ps1 command [args]

param(
    [Parameter(Position=0, Mandatory=$true)]
    [string]$Command,
    
    [Parameter(Position=1, ValueFromRemainingArguments=$true)]
    [string[]]$Arguments
)

Write-Host "Running git command: $Command $Arguments" -ForegroundColor Cyan

# Build the argument array
$gitArgs = @($Command)
if ($Arguments) {
    $gitArgs += $Arguments
}

# Execute git with all arguments
& "$env:ProgramFiles\Git\bin\git.exe" $gitArgs

# Return the exit code from git
exit $LASTEXITCODE 
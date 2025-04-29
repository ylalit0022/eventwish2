# Git function for PowerShell profile
# Add this to your PowerShell profile to make git easily accessible

function git {
    param(
        [Parameter(Position=0, Mandatory=$true)]
        [string]$Command,
        
        [Parameter(Position=1, ValueFromRemainingArguments=$true)]
        [string[]]$Arguments
    )
    
    # Build the argument array
    $gitArgs = @($Command)
    if ($Arguments) {
        $gitArgs += $Arguments
    }
    
    # Execute git with all arguments
    & "$env:ProgramFiles\Git\bin\git.exe" $gitArgs
    
    # Return the exit code from git
    return $LASTEXITCODE
} 
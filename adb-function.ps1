# ADB function for PowerShell profile
# Add this to your PowerShell profile to make ADB easily accessible

function adb {
    param(
        [Parameter(Position=0, Mandatory=$false)]
        [string]$Command,
        
        [Parameter(Position=1, ValueFromRemainingArguments=$true)]
        [string[]]$Arguments
    )
    
    # Build the argument array
    $adbArgs = @()
    if ($Command) {
        $adbArgs += $Command
    }
    if ($Arguments) {
        $adbArgs += $Arguments
    }
    
    # Execute ADB with all arguments
    & "D:\Sdk\platform-tools\adb.exe" $adbArgs
    
    # Return the exit code from ADB
    return $LASTEXITCODE
} 
@echo off
REM Helper script to make git commands easier to use
REM Usage: git.bat command [args]

echo Running git command: %*
"%ProgramFiles%\Git\bin\git.exe" %* 
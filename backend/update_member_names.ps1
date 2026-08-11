# PowerShell script to replace all occurrences of firstName + lastName concatenations with getFullName()
# This script updates all Java files in the backend to use the new fullName field

$javaFilesDir = "src\main\java\com\minet\sacco"
$pattern1 = '\.getFirstName\(\)\s*\+\s*"\s*"\s*\+\s*\.getLastName\(\)'
$replacement1 = '.getFullName()'

$pattern2 = 'member\.getFirstName\(\)\s*\+\s*"\s*"\s*\+\s*member\.getLastName\(\)'
$replacement2 = 'member.getFullName()'

$pattern3 = 'guarantor\.getFirstName\(\)\s*\+\s*"\s*"\s*\+\s*guarantor\.getLastName\(\)'
$replacement3 = 'guarantor.getFullName()'

Write-Host "Updating Java files to use fullName instead of firstName + lastName..."

$filesUpdated = 0
Get-ChildItem -Path $javaFilesDir -Recurse -Filter *.java | ForEach-Object {
    $file = $_
    $content = Get-Content $file.FullName -Raw
    $originalContent = $content
    
    # Apply all patterns
    $content = $content -replace $pattern1, $replacement1
    $content = $content -replace $pattern2, $replacement2
    $content = $content -replace $pattern3, $replacement3
    
    # If content changed, write it back
    if ($content -ne $originalContent) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "Updated: $($file.FullName)"
        $filesUpdated++
    }
}

Write-Host "`nTotal files updated: $filesUpdated"
Write-Host "Done!"

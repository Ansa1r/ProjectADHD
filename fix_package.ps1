$ErrorActionPreference = "Stop"

$root = $PSScriptRoot

if (-not $root) {
    $root = (Get-Location).Path
}

Set-Location $root

Write-Host "========================================"
Write-Host "ProjectADHD package rename"
Write-Host "Project root: $root"
Write-Host "========================================"
Write-Host ""

$oldPackage = "com.example.projectadhd"
$newPackage = "com.ansa1r.projectadhd"

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)

function Replace-InFile {
    param(
        [string]$Path,
        [string]$OldText,
        [string]$NewText
    )

    if (-not (Test-Path $Path)) {
        return
    }

    $content = [System.IO.File]::ReadAllText($Path)

    if ($content.Contains($OldText)) {
        $content = $content.Replace($OldText, $NewText)
        [System.IO.File]::WriteAllText($Path, $content, $utf8NoBom)
        Write-Host "Updated: $Path"
    }
}

Write-Host "Replacing package names..."

$extensions = @(
    "*.kt",
    "*.kts",
    "*.xml",
    "*.properties",
    "*.toml",
    "*.md",
    "*.txt"
)

foreach ($extension in $extensions) {
    Get-ChildItem `
        -Path $root `
        -Recurse `
        -File `
        -Filter $extension `
        -ErrorAction SilentlyContinue |
    Where-Object {
        $_.FullName -notmatch "\\\.git\\" -and
        $_.FullName -notmatch "\\\.gradle\\" -and
        $_.FullName -notmatch "\\\.idea\\" -and
        $_.FullName -notmatch "\\build\\"
    } |
    ForEach-Object {
        Replace-InFile $_.FullName $oldPackage $newPackage
        Replace-InFile $_.FullName "ProjectAHDH" "ProjectADHD"
    }
}

Write-Host ""
Write-Host "Moving source packages..."

$sourceSets = @(
    "main",
    "test",
    "androidTest"
)

$sourceRoots = @(
    "java",
    "kotlin"
)

foreach ($sourceSet in $sourceSets) {
    foreach ($sourceRoot in $sourceRoots) {

        $base = Join-Path $root "app\src\$sourceSet\$sourceRoot\com"

        $oldDir = Join-Path $base "example\projectadhd"
        $newDir = Join-Path $base "ansa1r\projectadhd"

        if (Test-Path $oldDir) {

            $newParent = Split-Path $newDir -Parent

            if (-not (Test-Path $newParent)) {
                New-Item `
                    -ItemType Directory `
                    -Path $newParent `
                    -Force |
                Out-Null
            }

            if (Test-Path $newDir) {

                Get-ChildItem $oldDir -Force | ForEach-Object {
                    Copy-Item `
                        $_.FullName `
                        $newDir `
                        -Recurse `
                        -Force
                }

                Remove-Item `
                    $oldDir `
                    -Recurse `
                    -Force
            }
            else {
                Move-Item `
                    $oldDir `
                    $newDir
            }

            Write-Host "Moved: $oldDir"
            Write-Host "    -> $newDir"

            $exampleDir = Join-Path $base "example"

            if (Test-Path $exampleDir) {
                $remaining = Get-ChildItem $exampleDir -Force

                if ($remaining.Count -eq 0) {
                    Remove-Item $exampleDir -Force
                }
            }
        }
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host "Checking old package references"
Write-Host "========================================"

$oldReferences = Get-ChildItem `
    -Path $root `
    -Recurse `
    -File `
    -ErrorAction SilentlyContinue |
Where-Object {
    $_.FullName -notmatch "\\\.git\\" -and
    $_.FullName -notmatch "\\\.gradle\\" -and
    $_.FullName -notmatch "\\\.idea\\" -and
    $_.FullName -notmatch "\\build\\" -and
    $_.Extension -in @(
        ".kt",
        ".kts",
        ".xml",
        ".properties",
        ".toml",
        ".md"
    )
} |
Select-String `
    -Pattern "com\.example\.projectadhd" `
    -ErrorAction SilentlyContinue

if ($oldReferences) {
    Write-Host ""
    Write-Host "WARNING: old package still found:"
    $oldReferences | ForEach-Object {
        Write-Host "$($_.Path):$($_.LineNumber)"
    }
}
else {
    Write-Host "No old package references found."
}

Write-Host ""
Write-Host "========================================"
Write-Host "Running Gradle clean"
Write-Host "========================================"

& ".\gradlew.bat" clean

if ($LASTEXITCODE -ne 0) {
    throw "Gradle clean failed."
}

Write-Host ""
Write-Host "========================================"
Write-Host "Running unit tests"
Write-Host "========================================"

& ".\gradlew.bat" testDebugUnitTest

if ($LASTEXITCODE -ne 0) {
    throw "Unit tests failed."
}

Write-Host ""
Write-Host "========================================"
Write-Host "Building debug APK"
Write-Host "========================================"

& ".\gradlew.bat" assembleDebug

if ($LASTEXITCODE -ne 0) {
    throw "Debug build failed."
}

Write-Host ""
Write-Host "========================================"
Write-Host "SUCCESS"
Write-Host "========================================"
Write-Host ""
Write-Host "Package:"
Write-Host "com.ansa1r.projectadhd"
Write-Host ""
Write-Host "APK:"
Write-Host "app\build\outputs\apk\debug\app-debug.apk"
Write-Host ""
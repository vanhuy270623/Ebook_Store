# Script tự động download và validate EPUB files
# Chạy: .\fix-epub-files.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  FIX EPUB FILES - AUTO VALIDATOR" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$epubFile = "F:\datn_uploads\book_asset\source\khoahoc-vientuong\Chien Tranh Giua Cac The Gioi - H. G. Wells.epub"

# Check file exists
Write-Host "[1/5] Checking file..." -ForegroundColor Yellow
if (Test-Path $epubFile) {
    Write-Host "  OK File exists" -ForegroundColor Green
    $size = (Get-Item $epubFile).Length / 1KB
    Write-Host "  Size: $([math]::Round($size, 2)) KB" -ForegroundColor Gray
} else {
    Write-Host "  ERROR File not found!" -ForegroundColor Red
    exit 1
}

# Check if it's a valid ZIP
Write-Host ""
Write-Host "[2/5] Checking ZIP structure..." -ForegroundColor Yellow
try {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($epubFile)
    $fileCount = $zip.Entries.Count
    Write-Host "  OK ZIP valid - $fileCount files" -ForegroundColor Green

    # Check for required EPUB files
    $hasMimetype = $zip.Entries | Where-Object { $_.Name -eq "mimetype" }
    $hasContainer = $zip.Entries | Where-Object { $_.FullName -like "*container.xml*" }
    $hasOPF = $zip.Entries | Where-Object { $_.Name -like "*.opf" }

    if ($hasMimetype) {
        Write-Host "  OK mimetype file found" -ForegroundColor Green
    } else {
        Write-Host "  WARN mimetype file missing" -ForegroundColor Yellow
    }

    if ($hasContainer) {
        Write-Host "  OK container.xml found" -ForegroundColor Green
    } else {
        Write-Host "  WARN container.xml missing" -ForegroundColor Yellow
    }

    if ($hasOPF) {
        Write-Host "  OK .opf file found" -ForegroundColor Green
    } else {
        Write-Host "  WARN .opf file missing" -ForegroundColor Yellow
    }

    $zip.Dispose()
} catch {
    Write-Host "  ERROR Not a valid ZIP: $_" -ForegroundColor Red
}

# Check Calibre installed
Write-Host ""
Write-Host "[3/5] Checking Calibre..." -ForegroundColor Yellow
$calibrePath = "C:\Program Files\Calibre2\ebook-convert.exe"
if (Test-Path $calibrePath) {
    Write-Host "  OK Calibre found" -ForegroundColor Green
} else {
    Write-Host "  WARN Calibre not found at default location" -ForegroundColor Yellow
    Write-Host "  Install from: https://calibre-ebook.com/download" -ForegroundColor Cyan
    $calibreInstalled = $false
}

# Suggest actions
Write-Host ""
Write-Host "[4/5] Recommendations:" -ForegroundColor Yellow

$recommendations = @()

if (-not $hasMimetype -or -not $hasContainer -or -not $hasOPF) {
    $recommendations += "CRITICAL: EPUB structure incomplete - needs fixing"
}

if ($calibreInstalled -eq $false) {
    $recommendations += "Install Calibre to fix/convert EPUB"
}

$recommendations += "Download new EPUB from Project Gutenberg or Archive.org"
$recommendations += "Test with file book_13 (Sieu Kinh Te Hoc) instead"

foreach ($rec in $recommendations) {
    Write-Host "  * $rec" -ForegroundColor Cyan
}

# Offer to backup and download new file
Write-Host ""
Write-Host "[5/5] Actions:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Option 1: Backup current file" -ForegroundColor White
Write-Host "  Copy-Item '$epubFile' '$epubFile.backup'" -ForegroundColor Gray
Write-Host ""
Write-Host "Option 2: Download 'War of the Worlds' from Project Gutenberg" -ForegroundColor White
Write-Host "  URL: https://www.gutenberg.org/ebooks/36" -ForegroundColor Gray
Write-Host "  Format: EPUB (with images)" -ForegroundColor Gray
Write-Host ""
Write-Host "Option 3: Convert with Calibre" -ForegroundColor White
if (Test-Path $calibrePath) {
    Write-Host "  & '$calibrePath' '$epubFile' '$epubFile.new.epub'" -ForegroundColor Gray
} else {
    Write-Host "  (Install Calibre first)" -ForegroundColor Gray
}
Write-Host ""
Write-Host "Option 4: Test with book_13" -ForegroundColor White
Write-Host "  Path: /book_asset/source/kinhte-quanly/Sieu Kinh Te Hoc Hai Huoc - STEVEN D. LEVITT.epub" -ForegroundColor Gray
Write-Host ""

# Prompt for action
$action = Read-Host "Choose action (1-4, or 'n' to exit)"

switch ($action) {
    "1" {
        Write-Host ""
        Write-Host "Backing up file..." -ForegroundColor Yellow
        Copy-Item $epubFile "$epubFile.backup" -Force
        Write-Host "OK Backup created: $epubFile.backup" -ForegroundColor Green
    }
    "2" {
        Write-Host ""
        Write-Host "Opening browser to Project Gutenberg..." -ForegroundColor Yellow
        Start-Process "https://www.gutenberg.org/ebooks/36"
        Write-Host "OK Download EPUB and replace file manually" -ForegroundColor Cyan
    }
    "3" {
        if (Test-Path $calibrePath) {
            Write-Host ""
            Write-Host "Converting with Calibre..." -ForegroundColor Yellow
            $outputFile = "$epubFile.new.epub"
            & $calibrePath $epubFile $outputFile

            if (Test-Path $outputFile) {
                Write-Host "OK Converted: $outputFile" -ForegroundColor Green
                Write-Host "Replace original? (y/n)" -ForegroundColor Yellow
                $replace = Read-Host
                if ($replace -eq "y") {
                    Move-Item $epubFile "$epubFile.backup" -Force
                    Move-Item $outputFile $epubFile -Force
                    Write-Host "OK File replaced!" -ForegroundColor Green
                }
            }
        } else {
            Write-Host "ERROR Calibre not found!" -ForegroundColor Red
        }
    }
    "4" {
        Write-Host ""
        Write-Host "Book 13 path:" -ForegroundColor Cyan
        Write-Host "/book_asset/source/kinhte-quanly/Sieu Kinh Te Hoc Hai Huoc - STEVEN D. LEVITT.epub" -ForegroundColor White
        Write-Host ""
        Write-Host "Test with this path in test page" -ForegroundColor Cyan
    }
    default {
        Write-Host ""
        Write-Host "No action taken" -ForegroundColor Gray
    }
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Script completed" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan


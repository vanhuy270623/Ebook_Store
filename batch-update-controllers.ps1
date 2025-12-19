# Batch update all User Controllers to extend BaseAdminController

$controllers = @(
    "ReadingController",
    "UserBookController",
    "SubscriptionController",
    "PaymentController",
    "OrderController",
    "FavoriteController",
    "BookDownloadController"
)

$baseDir = "C:\Projects\Ebook_Store\src\main\java\stu\datn\ebook_store\controller\user"

foreach ($controller in $controllers) {
    $filePath = "$baseDir\$controller.java"

    if (Test-Path $filePath) {
        Write-Host "Processing $controller..." -ForegroundColor Cyan

        $content = Get-Content $filePath -Raw -Encoding UTF8

        # 1. Add BaseAdminController import
        if ($content -match "package stu\.datn\.ebook_store\.controller\.user;") {
            $content = $content -replace "(package stu\.datn\.ebook_store\.controller\.user;[\r\n]+)",
                "`$1`nimport stu.datn.ebook_store.controller.admin.BaseAdminController;`n"
        }

        # 2. Remove Authentication import
        $content = $content -replace "import org\.springframework\.security\.core\.Authentication;[\r\n]+", ""

        # 3. Update class declaration to extend BaseAdminController
        $content = $content -replace "public class ($controller) \{", "public class `$1 extends BaseAdminController {"

        # 4. Remove private getCurrentUser method
        $content = $content -replace "(?s)\/\*\*[\s\S]*?Lấy user hiện tại.*?[\s\S]*?\*\/[\r\n\s]*private User getCurrentUser\(Authentication authentication\) \{[\s\S]*?return \(User\) authentication\.getPrincipal\(\);[\s\S]*?\}[\r\n]+", ""

        # 5. Replace getCurrentUser(authentication) with getCurrentUser()
        $content = $content -replace "getCurrentUser\(authentication\)", "getCurrentUser()"

        # 6. Remove Authentication parameter from method signatures (various patterns)
        $content = $content -replace ",[\s\r\n]+Authentication authentication", ""
        $content = $content -replace "Authentication authentication,[\s\r\n]+", ""
        $content = $content -replace "\(Authentication authentication\)", "()"

        # 7. Remove model.addAttribute("user", currentUser)
        $content = $content -replace 'model\.addAttribute\("user", currentUser\);[\r\n\s]*', ""

        # Save file
        Set-Content $filePath $content -NoNewline -Encoding UTF8
        Write-Host "✓ Updated $controller" -ForegroundColor Green
    } else {
        Write-Host "✗ File not found: $controller" -ForegroundColor Yellow
    }
}

Write-Host "`n✓ Batch update completed!" -ForegroundColor Green
Write-Host "Please check and compile the project." -ForegroundColor Cyan


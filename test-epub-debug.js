// ═══════════════════════════════════════════════════════════
// 🧪 SCRIPT KIỂM TRA EPUB VIEWER - PASTE VÀO CONSOLE
// ═══════════════════════════════════════════════════════════

console.clear();
console.log('%c🔍 KIỂM TRA EPUB VIEWER', 'font-size: 20px; font-weight: bold; color: #667eea;');
console.log('═'.repeat(60));

// 1. Kiểm tra ePub library
console.log('\n📦 1. KIỂM TRA EPUB.JS LIBRARY:');
if (typeof ePub !== 'undefined') {
    console.log('%c✅ ePub loaded', 'color: green; font-weight: bold;');
    console.log('   Version:', ePub.VERSION || 'Unknown');
    console.log('   Object:', ePub);
} else {
    console.log('%c❌ ePub NOT LOADED!', 'color: red; font-weight: bold;');
    console.log('   → CDN bị block hoặc chưa load xong');
}

// 2. Kiểm tra Thymeleaf variables
console.log('\n📋 2. KIỂM TRA THYMELEAF VARIABLES:');
try {
    console.log('   bookId:', typeof bookId !== 'undefined' ? bookId : '❌ undefined');
    console.log('   assetPath:', typeof assetPath !== 'undefined' ? assetPath : '❌ undefined');
    console.log('   initialLocation:', typeof initialLocation !== 'undefined' ? initialLocation : 'null (OK)');

    if (typeof assetPath !== 'undefined') {
        if (assetPath === '') {
            console.log('%c   ⚠️ assetPath TRỐNG!', 'color: red; font-weight: bold;');
        } else {
            console.log('%c   ✅ assetPath hợp lệ', 'color: green;');
        }
    }
} catch (e) {
    console.log('%c   ❌ Lỗi:', 'color: red;', e.message);
}

// 3. Kiểm tra DOM elements
console.log('\n🎨 3. KIỂM TRA DOM ELEMENTS:');
const epubViewerEl = document.getElementById('epub-viewer');
const loadingEl = document.getElementById('loadingOverlay');
console.log('   #epub-viewer:', epubViewerEl ? '✅ Exists' : '❌ Not found');
console.log('   #loadingOverlay:', loadingEl ? '✅ Exists' : '❌ Not found');

// 4. Kiểm tra book instances
console.log('\n📚 4. KIỂM TRA BOOK INSTANCES:');
if (typeof book !== 'undefined' && book) {
    console.log('%c✅ Book instance:', 'color: green; font-weight: bold;', book);
} else {
    console.log('   ⏳ Book chưa được khởi tạo (hoặc đang loading)');
}

if (typeof rendition !== 'undefined' && rendition) {
    console.log('%c✅ Rendition instance:', 'color: green; font-weight: bold;', rendition);
} else {
    console.log('   ⏳ Rendition chưa được khởi tạo');
}

// 5. Test file path
console.log('\n🔗 5. TEST FILE PATH:');
if (typeof assetPath !== 'undefined' && assetPath) {
    const fullUrl = window.location.origin + assetPath;
    console.log('   Full URL:', fullUrl);

    // Test accessibility
    console.log('   Testing accessibility...');
    fetch(assetPath, { method: 'HEAD' })
        .then(response => {
            if (response.ok) {
                console.log('%c   ✅ File accessible (HTTP ' + response.status + ')', 'color: green; font-weight: bold;');
                console.log('   Content-Type:', response.headers.get('content-type'));
                console.log('   Content-Length:', response.headers.get('content-length'), 'bytes');
            } else {
                console.log('%c   ❌ File NOT accessible (HTTP ' + response.status + ')', 'color: red; font-weight: bold;');
            }
        })
        .catch(err => {
            console.log('%c   ❌ Network error:', 'color: red; font-weight: bold;', err.message);
        });
} else {
    console.log('%c   ❌ assetPath không hợp lệ', 'color: red; font-weight: bold;');
}

// 6. Kiểm tra script tags
console.log('\n📜 6. KIỂM TRA SCRIPT TAGS:');
const epubScripts = document.querySelectorAll('script[src*="epub"]');
console.log('   Số script ePub.js:', epubScripts.length);
epubScripts.forEach((script, i) => {
    console.log(`   Script ${i+1}:`, script.src);
    console.log('     Loaded:', script.complete ? '✅' : '⏳');
});

// 7. Summary
console.log('\n' + '═'.repeat(60));
console.log('%c📊 TÓM TẮT:', 'font-size: 16px; font-weight: bold; color: #667eea;');

let issues = [];
let warnings = [];

if (typeof ePub === 'undefined') {
    issues.push('❌ ePub.js chưa load');
}
if (typeof assetPath === 'undefined' || assetPath === '') {
    issues.push('❌ assetPath không hợp lệ');
}
if (!epubViewerEl) {
    issues.push('❌ Element #epub-viewer không tồn tại');
}
if (typeof book === 'undefined' || !book) {
    warnings.push('⚠️ Book instance chưa tạo');
}

if (issues.length > 0) {
    console.log('%c🚨 CÓ VẤN ĐỀ NGHIÊM TRỌNG:', 'color: red; font-weight: bold;');
    issues.forEach(issue => console.log('   ' + issue));
} else if (warnings.length > 0) {
    console.log('%c⚠️ CÓ CẢNH BÁO:', 'color: orange; font-weight: bold;');
    warnings.forEach(warn => console.log('   ' + warn));
    console.log('   (Có thể đang trong quá trình load)');
} else {
    console.log('%c✅ TẤT CẢ ĐỀU TỐT!', 'color: green; font-weight: bold;');
    console.log('   Nếu sách vẫn không load, kiểm tra Network tab');
}

console.log('\n' + '═'.repeat(60));
console.log('💡 TIP: Mở Network tab (F12) để xem chi tiết request');
console.log('═'.repeat(60));


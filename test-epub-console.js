// 🔍 SCRIPT KIỂM TRA CDN EPUB.JS TRONG CONSOLE
// Copy và paste vào Console của trình duyệt (F12) khi đang ở trang epub-viewer

console.log('🔍 BẮT ĐẦU KIỂM TRA CDN EPUB.JS...\n');

// 1. Kiểm tra ePub object
if (typeof ePub !== 'undefined') {
    console.log('✅ ePub object đã được load thành công!');
    console.log('📦 ePub object:', ePub);

    // Kiểm tra version
    if (ePub.VERSION) {
        console.log('📌 Version:', ePub.VERSION);
    }

    // Kiểm tra các methods/properties quan trọng
    const requiredMethods = ['Book', 'Rendition', 'Layout', 'Contents'];
    console.log('\n📋 Kiểm tra các methods cần thiết:');

    requiredMethods.forEach(method => {
        if (typeof ePub[method] !== 'undefined') {
            console.log(`  ✅ ${method}: có sẵn`);
        } else {
            console.log(`  ❌ ${method}: KHÔNG TÌM THẤY`);
        }
    });

    // List tất cả properties
    console.log('\n📝 Tất cả properties của ePub:');
    console.log(Object.keys(ePub));

} else {
    console.error('❌ EPUB.JS CHƯA ĐƯỢC LOAD!');
    console.log('\n🔧 Các bước debug:');
    console.log('1. Kiểm tra Network tab xem CDN có load thành công không');
    console.log('2. Tìm request: cdn.jsdelivr.net/npm/epubjs');
    console.log('3. Xem Status Code (phải là 200)');
    console.log('4. Kiểm tra Console có lỗi CORS không');
}

// 2. Kiểm tra CDN đã load chưa
console.log('\n🌐 Kiểm tra CDN Scripts:');
const scripts = document.querySelectorAll('script[src*="epub"]');
if (scripts.length > 0) {
    console.log(`✅ Tìm thấy ${scripts.length} script(s) ePub.js:`);
    scripts.forEach((script, index) => {
        console.log(`  ${index + 1}. ${script.src}`);
        console.log(`     - Đã load: ${script.complete ? '✅' : '⏳'}`);
    });
} else {
    console.error('❌ KHÔNG TÌM THẤY SCRIPT EPUB.JS!');
}

// 3. Kiểm tra book instance (nếu đã khởi tạo)
console.log('\n📚 Kiểm tra Book instance:');
if (typeof book !== 'undefined' && book) {
    console.log('✅ Book instance đã được khởi tạo');
    console.log('📖 Book:', book);
} else {
    console.log('⏳ Book instance chưa được khởi tạo (có thể đang load)');
}

// 4. Kiểm tra rendition (nếu đã khởi tạo)
console.log('\n🎨 Kiểm tra Rendition:');
if (typeof rendition !== 'undefined' && rendition) {
    console.log('✅ Rendition đã được khởi tạo');
    console.log('🖼️ Rendition:', rendition);
} else {
    console.log('⏳ Rendition chưa được khởi tạo');
}

// 5. Test khả năng tạo Book mới
console.log('\n🧪 Test tạo Book instance:');
try {
    const testBook = ePub();
    console.log('✅ Có thể tạo Book instance mới');
    console.log('📦 Test Book:', testBook);
} catch (error) {
    console.error('❌ Lỗi khi tạo Book instance:', error);
}

console.log('\n✅ HOÀN TẤT KIỂM TRA!');
console.log('═'.repeat(50));


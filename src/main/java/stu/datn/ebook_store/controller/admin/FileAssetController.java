package stu.datn.ebook_store.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import stu.datn.ebook_store.controller.BaseController;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Controller quản lý file assets (hình ảnh, PDF, EPUB)
 * Cho phép admin xem, tìm kiếm và xóa các file đã upload
 */
@Controller
@RequestMapping("/admin/books/assets")
public class FileAssetController extends BaseController {

    @Value("${file.upload-dir:F:/datn_uploads}")
    private String uploadDir;

    /**
     * Hiển thị trang quản lý file
     * GET /admin/books/assets
     */
    @GetMapping
    public String assetsPage(
            @RequestParam(value = "type", required = false, defaultValue = "all") String type,
            @RequestParam(value = "search", required = false) String search,
            Model model) {

        try {
            List<FileAssetInfo> files = new ArrayList<>();

            // Quét tất cả thư mục
            Path basePath = Paths.get(uploadDir, "book_asset");

            if (Files.exists(basePath)) {
                // Quét thư mục covers
                if ("all".equals(type) || "covers".equals(type)) {
                    Path coversPath = basePath.resolve("image/covers");
                    if (Files.exists(coversPath)) {
                        files.addAll(scanDirectory(coversPath, "covers", search));
                    }
                }

                // Quét thư mục source
                if ("all".equals(type) || "source".equals(type)) {
                    Path sourcePath = basePath.resolve("source");
                    if (Files.exists(sourcePath)) {
                        files.addAll(scanDirectory(sourcePath, "source", search));
                    }
                }

                // Quét thư mục preview
                if ("all".equals(type) || "preview".equals(type)) {
                    Path previewPath = basePath.resolve("preview");
                    if (Files.exists(previewPath)) {
                        files.addAll(scanDirectory(previewPath, "preview", search));
                    }
                }

                // Quét thư mục avatars
                if ("all".equals(type) || "avatars".equals(type)) {
                    Path avatarsPath = basePath.resolve("image/avatars");
                    if (Files.exists(avatarsPath)) {
                        files.addAll(scanDirectory(avatarsPath, "avatars", search));
                    }
                }

                // Quét thư mục authors
                if ("all".equals(type) || "authors".equals(type)) {
                    Path authorsPath = basePath.resolve("image/authors");
                    if (Files.exists(authorsPath)) {
                        files.addAll(scanDirectory(authorsPath, "authors", search));
                    }
                }

                // Quét thư mục banners
                if ("all".equals(type) || "banners".equals(type)) {
                    Path bannersPath = basePath.resolve("image/banners");
                    if (Files.exists(bannersPath)) {
                        files.addAll(scanDirectory(bannersPath, "banners", search));
                    }
                }
            }

            // Sắp xếp theo ngày tạo (mới nhất trước)
            files.sort((f1, f2) -> f2.getLastModified().compareTo(f1.getLastModified()));

            // Tính toán thống kê
            long totalSize = files.stream().mapToLong(FileAssetInfo::getSize).sum();
            Map<String, Long> typeStats = files.stream()
                    .collect(Collectors.groupingBy(FileAssetInfo::getType, Collectors.counting()));

            model.addAttribute("files", files);
            model.addAttribute("totalFiles", files.size());
            model.addAttribute("totalSize", formatFileSize(totalSize));
            model.addAttribute("typeStats", typeStats);
            model.addAttribute("currentType", type);
            model.addAttribute("searchQuery", search);

            return "admin/books/assets";

        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi quét thư mục: " + e.getMessage());
            model.addAttribute("files", new ArrayList<>());
            return "admin/books/assets";
        }
    }

    /**
     * Xóa file
     * DELETE /admin/books/assets/delete
     */
    @DeleteMapping("/delete")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam("path") String filePath) {
        Map<String, Object> response = new HashMap<>();

        try {
            Path fullPath = Paths.get(uploadDir, "book_asset").resolve(filePath);

            // Kiểm tra file có tồn tại không
            if (!Files.exists(fullPath)) {
                response.put("success", false);
                response.put("message", "File không tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            // Xóa file
            Files.delete(fullPath);

            response.put("success", true);
            response.put("message", "Đã xóa file thành công!");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi xóa file: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Lấy thông tin chi tiết file
     * GET /admin/books/assets/info
     */
    @GetMapping("/info")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFileInfo(@RequestParam("path") String filePath) {
        Map<String, Object> response = new HashMap<>();

        try {
            Path fullPath = Paths.get(uploadDir, "book_asset").resolve(filePath);

            if (!Files.exists(fullPath)) {
                response.put("success", false);
                response.put("message", "File không tồn tại!");
                return ResponseEntity.badRequest().body(response);
            }

            BasicFileAttributes attrs = Files.readAttributes(fullPath, BasicFileAttributes.class);

            response.put("success", true);
            response.put("name", fullPath.getFileName().toString());
            response.put("path", filePath);
            response.put("size", attrs.size());
            response.put("sizeFormatted", formatFileSize(attrs.size()));
            response.put("created", attrs.creationTime().toString());
            response.put("modified", attrs.lastModifiedTime().toString());
            response.put("isDirectory", attrs.isDirectory());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi khi lấy thông tin file: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Quét thư mục và lấy danh sách file
     */
    private List<FileAssetInfo> scanDirectory(Path directory, String type, String search) throws IOException {
        List<FileAssetInfo> files = new ArrayList<>();

        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                String fileName = file.getFileName().toString();

                // Bỏ qua hidden files
                if (fileName.startsWith(".")) {
                    return FileVisitResult.CONTINUE;
                }

                // Lọc theo search query
                if (search != null && !search.trim().isEmpty()) {
                    if (!fileName.toLowerCase().contains(search.toLowerCase())) {
                        return FileVisitResult.CONTINUE;
                    }
                }

                FileAssetInfo info = new FileAssetInfo();
                info.setName(fileName);
                info.setType(type);
                info.setSize(attrs.size());
                info.setLastModified(new Date(attrs.lastModifiedTime().toMillis()));

                // Tạo relative path
                Path basePath = Paths.get(uploadDir, "book_asset");
                String relativePath = basePath.relativize(file).toString().replace("\\", "/");
                info.setPath(relativePath);

                // Xác định extension
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    info.setExtension(fileName.substring(dotIndex + 1).toLowerCase());
                }

                files.add(info);
                return FileVisitResult.CONTINUE;
            }
        });

        return files;
    }

    /**
     * Format file size thành chuỗi dễ đọc
     */
    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";

        final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

        return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups))
                + " " + units[digitGroups];
    }

    /**
     * Inner class để lưu thông tin file
     */
    public static class FileAssetInfo {
        private String name;
        private String path;
        private String type;
        private String extension;
        private long size;
        private Date lastModified;

        // Getters and setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getExtension() {
            return extension;
        }

        public void setExtension(String extension) {
            this.extension = extension;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public Date getLastModified() {
            return lastModified;
        }

        public void setLastModified(Date lastModified) {
            this.lastModified = lastModified;
        }

        public String getFormattedSize() {
            if (size <= 0) return "0 B";

            final String[] units = new String[] { "B", "KB", "MB", "GB", "TB" };
            int digitGroups = (int) (Math.log10(size) / Math.log10(1024));

            return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups))
                    + " " + units[digitGroups];
        }
    }
}


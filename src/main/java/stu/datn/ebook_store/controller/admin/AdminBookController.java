package stu.datn.ebook_store.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import stu.datn.ebook_store.dto.BookDTO;
import stu.datn.ebook_store.entity.Author;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.service.BookService;
import stu.datn.ebook_store.service.CategoryService;
import stu.datn.ebook_store.service.AuthorService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/books")
public class AdminBookController {

    private final BookService bookService;
    private final CategoryService categoryService;
    private final AuthorService authorService;

    @Autowired
    public AdminBookController(BookService bookService, CategoryService categoryService, AuthorService authorService) {
        this.bookService = bookService;
        this.categoryService = categoryService;
        this.authorService = authorService;
    }

    @GetMapping
    public String booksList(Model model) {
        List<Book> books = bookService.getAllBooks();
        model.addAttribute("books", books);
        model.addAttribute("totalBooks", books.size());
        return "admin/books/list";
    }

    @GetMapping("/add")
    public String addBookForm(Model model) {
        model.addAttribute("book", new BookDTO());
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("authors", authorService.getAllAuthors());
        model.addAttribute("accessTypes", Book.AccessType.values());
        return "admin/books/add";
    }

    @PostMapping("/add")
    public String addBook(@ModelAttribute BookDTO bookDTO,
                          @RequestParam(value = "authorIds", required = false) Set<String> authorIds,
                          @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                          RedirectAttributes redirectAttributes) {
        try {
            // Upload cover image if provided
            if (coverImage != null && !coverImage.isEmpty()) {
                String imageUrl = bookService.uploadCoverImage(coverImage);
                bookDTO.setCoverImageUrl(imageUrl);
            }

            bookService.createBook(bookDTO, authorIds);
            redirectAttributes.addFlashAttribute("success", "Thêm sách mới thành công!");
            return "redirect:/admin/books";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/books/add";
        }
    }

    @GetMapping("/edit/{id}")
    public String editBookForm(@PathVariable String id, Model model) {
        Book book = bookService.getBookById(id).orElse(null);
        if (book == null) {
            return "redirect:/admin/books?error=notfound";
        }

        BookDTO bookDTO = new BookDTO(book);
        model.addAttribute("book", bookDTO);
        model.addAttribute("bookEntity", book);
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("authors", authorService.getAllAuthors());
        model.addAttribute("accessTypes", Book.AccessType.values());

        // Get current author IDs
        Set<String> currentAuthorIds = book.getAuthors().stream()
            .map(Author::getAuthorId)
            .collect(Collectors.toSet());
        model.addAttribute("currentAuthorIds", currentAuthorIds);

        return "admin/books/edit";
    }

    @PostMapping("/edit/{id}")
    public String editBook(@PathVariable String id,
                           @ModelAttribute BookDTO bookDTO,
                           @RequestParam(value = "authorIds", required = false) Set<String> authorIds,
                           @RequestParam(value = "coverImage", required = false) MultipartFile coverImage,
                           RedirectAttributes redirectAttributes) {
        try {
            // Upload cover image if provided
            if (coverImage != null && !coverImage.isEmpty()) {
                String imageUrl = bookService.uploadCoverImage(coverImage);
                bookDTO.setCoverImageUrl(imageUrl);
            }

            bookService.updateBook(id, bookDTO, authorIds);
            redirectAttributes.addFlashAttribute("success", "Cập nhật sách thành công!");
            return "redirect:/admin/books";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/books/edit/" + id;
        }
    }

    @GetMapping("/view/{id}")
    public String viewBook(@PathVariable String id, Model model) {
        Book book = bookService.getBookById(id).orElse(null);
        if (book == null) {
            return "redirect:/admin/books?error=notfound";
        }
        model.addAttribute("book", book);
        return "admin/books/view";
    }

    @PostMapping("/delete/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> deleteBook(@PathVariable String id) {
        Map<String, Object> response = new HashMap<>();
        try {
            bookService.deleteBook(id);
            response.put("success", true);
            response.put("message", "Xóa sách thành công!");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/upload-cover")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadCoverImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            String imageUrl = bookService.uploadCoverImage(file);
            response.put("success", true);
            response.put("url", imageUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/statistics")
    public String booksStatistics(Model model) {
        model.addAttribute("totalBooks", bookService.getTotalBooksCount());
        model.addAttribute("freeBooks", bookService.getFreeBooks());
        model.addAttribute("paidBooks", bookService.getPaidBooks());
        model.addAttribute("subscriptionBooks", bookService.getSubscriptionBooks());
        model.addAttribute("recentBooks", bookService.getRecentBooks(10));
        model.addAttribute("topRatedBooks", bookService.getTopRatedBooks(10));
        return "admin/books/statistics";
    }
}

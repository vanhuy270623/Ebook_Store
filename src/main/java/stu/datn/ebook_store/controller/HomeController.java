package stu.datn.ebook_store.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.service.BookService;

import java.util.List;

@Controller
public class HomeController {

    @Autowired
    private BookService bookService;

    @GetMapping("/")
    public String home(Model model) {
        try {
            // Get free books (ACCESS_TYPE = 'FREE')
            List<Book> freeBooks = bookService.getBooksByAccessType(Book.AccessType.FREE);

            // Get trending books (top viewed)
            List<Book> trendingBooks = bookService.getTopViewedBooks();

            // Get new releases
            List<Book> newBooks = bookService.getNewestBooks();

            // Add to model
            model.addAttribute("freeBooks", freeBooks);
            model.addAttribute("trendingBooks", trendingBooks);
            model.addAttribute("newBooks", newBooks);

        } catch (Exception e) {
            // Log error but still show the page
            System.err.println("Error loading books: " + e.getMessage());
        }

        return "home";
    }

    @GetMapping("/home")
    public String homeAlias() {
        return "redirect:/";
    }
}


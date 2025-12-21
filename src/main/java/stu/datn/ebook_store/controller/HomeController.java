package stu.datn.ebook_store.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import stu.datn.ebook_store.entity.Banner;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.service.BannerService;
import stu.datn.ebook_store.service.BookService;

import java.util.*;

@Controller
public class HomeController {

    @Autowired
    private BookService bookService;

    @Autowired
    private BannerService bannerService;

    @GetMapping("/")
    public String home(Model model) {
        // Authentication info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated() &&
            auth.getPrincipal() instanceof stu.datn.ebook_store.entity.User user) {
            model.addAttribute("user", user);
        }

        try {
            // Load active banners for HOME position (with date filtering and ordering)
            List<Banner> homeBanners = bannerService.getActiveBannersForDisplay(Banner.BannerPosition.HOME);
            model.addAttribute("banners", homeBanners);

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


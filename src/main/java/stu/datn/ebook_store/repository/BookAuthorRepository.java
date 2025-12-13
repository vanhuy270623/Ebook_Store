package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.datn.ebook_store.entity.Author;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookAuthor;
import stu.datn.ebook_store.entity.BookAuthorId;

import java.util.List;

public interface BookAuthorRepository extends JpaRepository<BookAuthor, BookAuthorId> {
    List<BookAuthor> findByBook(Book book);

    List<BookAuthor> findByAuthor(Author author);
}


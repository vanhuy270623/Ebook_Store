package stu.datn.ebook_store.service;

import stu.datn.ebook_store.entity.Author;

import java.util.List;
import java.util.Optional;

public interface AuthorService {
    List<Author> getAllAuthors();
    Optional<Author> getAuthorById(String authorId);
    Optional<Author> getAuthorByName(String name);
    Author saveAuthor(Author author);
    void deleteAuthor(String authorId);
    List<Author> searchAuthorsByName(String keyword);
}


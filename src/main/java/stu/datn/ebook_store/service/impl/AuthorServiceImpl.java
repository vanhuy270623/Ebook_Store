package stu.datn.ebook_store.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import stu.datn.ebook_store.entity.Author;
import stu.datn.ebook_store.repository.AuthorRepository;
import stu.datn.ebook_store.service.AuthorService;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class AuthorServiceImpl implements AuthorService {

    private final AuthorRepository authorRepository;

    @Autowired
    public AuthorServiceImpl(AuthorRepository authorRepository) {
        this.authorRepository = authorRepository;
    }

    @Override
    public List<Author> getAllAuthors() {
        return authorRepository.findAll();
    }

    @Override
    public Optional<Author> getAuthorById(String authorId) {
        return authorRepository.findById(authorId);
    }

    @Override
    public Optional<Author> getAuthorByName(String name) {
        return authorRepository.findByName(name);
    }

    @Override
    public Author saveAuthor(Author author) {
        if (author.getAuthorId() == null || author.getAuthorId().isEmpty()) {
            author.setAuthorId(generateAuthorId());
        }
        return authorRepository.save(author);
    }

    @Override
    public void deleteAuthor(String authorId) {
        authorRepository.deleteById(authorId);
    }

    @Override
    public List<Author> searchAuthorsByName(String keyword) {
        return authorRepository.searchByName(keyword);
    }

    private String generateAuthorId() {
        long count = authorRepository.count();
        return "author_" + System.currentTimeMillis() + "_" + (count + 1);
    }
}


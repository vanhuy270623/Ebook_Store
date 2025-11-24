package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import stu.datn.ebook_store.entity.Author;

import java.util.List;
import java.util.Optional;

public interface AuthorRepository extends JpaRepository<Author, String> {
    Optional<Author> findByName(String name);

    @Query("SELECT a FROM Author a WHERE a.name LIKE %:keyword%")
    List<Author> searchByName(@Param("keyword") String keyword);
}


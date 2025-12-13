package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import stu.datn.ebook_store.entity.Author;

import java.time.LocalDateTime;

/**
 * DTO for Author response
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponse {
    private String authorId;
    private String name;
    private String biography;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private Integer totalBooks; // Số sách của tác giả

    // Constructor from Entity
    public AuthorResponse(Author author) {
        this.authorId = author.getAuthorId();
        this.name = author.getName();
        this.biography = author.getBiography();
        this.avatarUrl = author.getAvatarUrl();
        this.createdAt = author.getCreatedAt();
    }

    // Constructor with book count
    public AuthorResponse(Author author, Integer totalBooks) {
        this(author);
        this.totalBooks = totalBooks;
    }
}


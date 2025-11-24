package stu.datn.ebook_store.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "category")
public class Category {
    @Id
    @Column(name = "category_id", length = 50)
    private String categoryId;

    @Column(name = "category_name", nullable = false, length = 255)
    private String categoryName;
}


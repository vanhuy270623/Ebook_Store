package stu.datn.ebook_store.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import stu.datn.ebook_store.entity.Book;
import stu.datn.ebook_store.entity.BookAsset;

import java.util.List;
import java.util.Optional;

public interface BookAssetRepository extends JpaRepository<BookAsset, String> {
    List<BookAsset> findByBook(Book book);

    Optional<BookAsset> findByBookAndFileType(Book book, BookAsset.FileType fileType);

    List<BookAsset> findByFileType(BookAsset.FileType fileType);
}


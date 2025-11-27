package stu.datn.ebook_store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Generic Pagination Response wrapper for list endpoints
 * @param <T> Type of data items
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> content;
    private Integer pageNumber;
    private Integer pageSize;
    private Long totalElements;
    private Integer totalPages;
    private Boolean last;
    private Boolean first;

    // Constructor from Spring Page object
    public PageResponse(org.springframework.data.domain.Page<T> page) {
        this.content = page.getContent();
        this.pageNumber = page.getNumber();
        this.pageSize = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.last = page.isLast();
        this.first = page.isFirst();
    }

    // Constructor for manual pagination
    public static <T> PageResponse<T> of(List<T> content, Integer pageNumber, Integer pageSize, Long totalElements) {
        PageResponse<T> response = new PageResponse<>();
        response.setContent(content);
        response.setPageNumber(pageNumber);
        response.setPageSize(pageSize);
        response.setTotalElements(totalElements);
        response.setTotalPages((int) Math.ceil((double) totalElements / pageSize));
        response.setFirst(pageNumber == 0);
        response.setLast(pageNumber >= response.getTotalPages() - 1);
        return response;
    }
}


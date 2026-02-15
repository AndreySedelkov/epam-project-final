package com.epam.finaltask.paged;

import lombok.Data;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
public class PagedResponse<T> {

    private List<T> content;

    private int page;

    private int size;

    private long totalElements;

    private int totalPages;

    private boolean last;


    public PagedResponse(Page<T> pg) {

        this.content = pg.getContent();

        this.page = pg.getNumber();

        this.size = pg.getSize();

        this.totalElements = pg.getTotalElements();

        this.totalPages = pg.getTotalPages();

        this.last = pg.isLast();
    }
}
package com.example.smartcustomerservice.common.result;

import java.util.Collections;
import java.util.List;

public class PageResult<T> {

    private List<T> records;
    private Long page;
    private Long size;
    private Long total;

    public PageResult() {
    }

    public PageResult(List<T> records, Long page, Long size, Long total) {
        this.records = records == null ? Collections.emptyList() : records;
        this.page = page;
        this.size = size;
        this.total = total;
    }

    public static <T> PageResult<T> of(List<T> records, Long page, Long size, Long total) {
        return new PageResult<>(records, page, size, total);
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }
}

package com.example.common.response;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页响应对象。
 *
 * @param <T> 列表数据类型
 */
public class PageResult<T> {

    /** 当前页码，从 1 开始。 */
    private Integer pageNum;

    /** 每页记录数。 */
    private Integer pageSize;

    /** 符合条件的记录总数。 */
    private Long total;

    /** 当前页数据。 */
    private List<T> records;

    public PageResult() {
    }

    public PageResult(Integer pageNum, Integer pageSize, Long total, List<T> records) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.records = records == null ? Collections.<T>emptyList() : records;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }
}

package com.echospace.common;

import java.util.List;

public class PageResult<T> {

    private int code;
    private String msg;
    private List<T> records;
    private long total;
    private int page;
    private int size;

    private PageResult() {}

    public static <T> PageResult<T> of(List<T> records, long total, int page, int size) {
        PageResult<T> r = new PageResult<>();
        r.code = 1;
        r.msg = "success";
        r.records = records;
        r.total = total;
        r.page = page;
        r.size = size;
        return r;
    }

    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public List<T> getRecords() { return records; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}

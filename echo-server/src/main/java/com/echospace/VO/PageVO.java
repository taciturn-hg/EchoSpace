package com.echospace.VO;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageVO<T> {
    private List<T> records;
    private long total;
    private long current;
    private long size;

    public static <T> PageVO<T> of(Page page) {
        return new PageVO<>(
                page.getRecords(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }
}

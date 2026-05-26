package com.echospace.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursorPageVO<T> {
    private List<T> records;
    private String cursor;
    private boolean hasMore;
    private int size;
}

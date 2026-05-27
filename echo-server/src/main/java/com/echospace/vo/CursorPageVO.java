package com.echospace.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 游标分页通用响应包装
 * <p>
 * cursor 为下一页游标，hasMore=false 或 cursor=null 表示已是最后一页。
 * </p>
 *
 * @param <T> 列表项类型
 * @Author: taciturn-hg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursorPageVO<T> {
    private List<T> records;
    private String cursor;
    private boolean hasMore;
    /** 本次返回的实际记录数 */
    private int count;
}

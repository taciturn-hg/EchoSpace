package com.echospace.service;

import com.echospace.vo.PageVO;
import com.echospace.vo.PostItemVO;

/**
 * 搜索服务接口：基于 Elasticsearch 对帖子进行全文检索
 *
 * @Author: taciturn-hg
 */
public interface SearchService {

    /**
     * 全文搜索帖子
     * <p>
     * 在标题和正文中检索关键词，通过 ik 分词器支持中文分词。
     * 返回结果中标题和正文匹配片段以 {@code <em>} 标签高亮。
     * </p>
     *
     * @param keyword 搜索关键词
     * @param current 页码，从 1 开始
     * @param size    每页条数
     * @param sort    排序方式：{@code created_at}=最新，{@code hot}=按点赞数
     * @return 页码分页结果，含总记录数和当前页数据
     */
    PageVO<PostItemVO> search(String keyword, int current, int size, String sort);
}

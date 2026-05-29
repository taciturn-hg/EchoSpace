package com.echospace.service.impl;

import com.echospace.document.PostDocument;
import com.echospace.service.SearchService;
import com.echospace.vo.AuthorVO;
import com.echospace.vo.PageVO;
import com.echospace.vo.PostItemVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.stereotype.Service;

import java.util.List;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 搜索服务实现：基于 Elasticsearch 对帖子标题和正文进行全文检索
 * <p>
 * 搜索流程：
 * <ol>
 *   <li>构建 Criteria 多字段匹配查询（title + contentText），ES 侧通过 ik 分词器进行中文分词</li>
 *   <li>配置高亮参数，匹配词片段以 {@code <em>...</em>} 标签包裹返回</li>
 *   <li>按指定字段排序后执行分页检索</li>
 *   <li>将 ES 返回的 {@link PostDocument} 映射为 {@link PostItemVO}，
 *       优先使用高亮片段替换原标题/正文</li>
 * </ol>
 * </p>
 *
 * @Author: taciturn-hg
 */
@Slf4j
@Service
public class SearchServiceImpl implements SearchService {

    @Autowired
    private ElasticsearchOperations esOps;

    @Override
    public PageVO<PostItemVO> search(String keyword, int current, int size, String sort) {
        String q = keyword != null ? keyword.trim() : "";

        String sortField = "hot".equals(sort) ? "likeCount" : "createdAt";

        String dsl;
        if (q.isEmpty()) {
            dsl = "{\"match_all\":{}}";
        } else {
            dsl = "{\"multi_match\":{"
                    + "\"query\":\"" + escapeJson(q) + "\","
                    + "\"fields\":[\"title\",\"contentText\"],"
                    + "\"fuzziness\":\"AUTO\""
                    + "}}";
        }

        StringQuery query = new StringQuery(dsl);
        query.setPageable(PageRequest.of(current - 1, size));
        query.addSort(Sort.by(Sort.Order.desc(sortField)));
        query.setHighlightQuery(buildHighlightQuery());

        log.info("ES 搜索 keyword={}, page={}, size={}, sort={}", q, current, size, sort);
        SearchHits<PostDocument> hits;
        try {
            hits = esOps.search(query, PostDocument.class);
        } catch (NoSuchIndexException e) {
            log.warn("ES 索引 posts 不存在，返回空结果");
            return new PageVO<>(List.of(), 0, current, size);
        }

        List<PostItemVO> records = hits.getSearchHits().stream()
                .map(this::mapToVO)
                .toList();

        log.info("ES 搜索完成 keyword={}, totalHits={}, returned={}", q, hits.getTotalHits(), records.size());
        return new PageVO<>(records, hits.getTotalHits(), current, size);
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 10);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"':  sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:   sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 构建高亮查询配置：匹配词用 {@code <em>} 标签包裹，高亮字段为标题和正文
     */
    private HighlightQuery buildHighlightQuery() {
        HighlightParameters params = HighlightParameters.builder()
                .withPreTags("<em>")
                .withPostTags("</em>")
                .build();
        Highlight highlight = new Highlight(params, List.of(
                new HighlightField("title"),
                new HighlightField("contentText")
        ));
        return new HighlightQuery(highlight, PostDocument.class);
    }

    /**
     * 将 ES 搜索结果映射为 PostItemVO，优先使用高亮片段
     */
    private PostItemVO mapToVO(SearchHit<PostDocument> hit) {
        PostDocument doc = hit.getContent();

        AuthorVO author = new AuthorVO(
                doc.getUserId(),
                doc.getUsername(),
                doc.getNickname(),
                doc.getAvatar()
        );

        PostItemVO vo = new PostItemVO();
        vo.setId(doc.getId());
        vo.setTitle(resolveHighlight(hit, "title", doc.getTitle()));
        vo.setCoverImage(doc.getCoverImage());
        vo.setContentText(resolveHighlight(hit, "contentText", doc.getContentText()));
        vo.setAuthor(author);
        vo.setLikeCount(doc.getLikeCount());
        vo.setCommentCount(doc.getCommentCount());
        vo.setCollectCount(doc.getCollectCount());
        vo.setCreatedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(doc.getCreatedAt()), ZoneId.of("Asia/Shanghai")));
        return vo;
    }

    /**
     * 优先取 ES 高亮片段，若无高亮则回退到原始值
     */
    private String resolveHighlight(SearchHit<PostDocument> hit, String field, String fallback) {
        List<String> fragments = hit.getHighlightField(field);
        if (fragments != null && !fragments.isEmpty()) {
            return fragments.get(0);
        }
        return fallback;
    }
}

package com.echospace.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.Setting;

/**
 * Elasticsearch 帖子索引映射实体
 * <p>
 * 索引名称为 {@code posts}，对应 MySQL 中已发布的非删除帖子。
 * 标题和正文使用 {@code ik_max_word} 做索引分词（细粒度切分），{@code ik_smart} 做搜索分词（粗粒度切分）。
 * 用户名/昵称为精确匹配（keyword），不参与分词。
 * createdAt 使用 epoch 毫秒（Long）存储，ES 原生支持 Date 类型排序，Java 侧无序列化歧义。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "posts")
@Setting(replicas = 0, shards = 1)
public class PostDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String contentText;

    private String coverImage;

    private Long userId;

    @Field(type = FieldType.Keyword)
    private String username;

    @Field(type = FieldType.Keyword)
    private String nickname;

    private String avatar;

    private Integer likeCount;

    private Integer commentCount;

    private Integer collectCount;

    @Field(type = FieldType.Date)
    private Long createdAt;
}

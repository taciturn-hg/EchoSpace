package com.echospace.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 帖子列表项视图对象，用于首页信息流展示
 *
 * @Author: taciturn-hg
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostItemVO {
    private Long id;
    private String title;
    private String coverImage;
    private String contentText;
    private AuthorVO author;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private Boolean isLiked;
    private Boolean isCollected;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

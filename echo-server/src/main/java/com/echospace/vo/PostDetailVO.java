package com.echospace.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PostDetailVO {
    private Long id;
    private String title;
    private String contentHtml;
    private AuthorVO author;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;
    private Integer viewCount;
    private Boolean isLiked;
    private Boolean isCollected;
    private Boolean isFollowed;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}

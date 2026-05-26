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
public class PostItemVO {
    private Long id;
    private String title;
    private String coverImage;
    private String contentText;
    private AuthorVO author;
    private Integer likeCount;
    private Integer commentCount;
    private Integer collectCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

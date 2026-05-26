package com.echospace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echospace.entity.Post;
import com.echospace.vo.PostDetailVO;
import com.echospace.vo.PostItemVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PostMapper extends BaseMapper<Post> {

    List<PostItemVO> selectListLatest(@Param("cursorTime") LocalDateTime cursorTime,
                                      @Param("cursorId") Long cursorId,
                                      @Param("size") int size);

    List<PostItemVO> selectListHot(@Param("cursorCount") Integer cursorCount,
                                   @Param("cursorId") Long cursorId,
                                   @Param("size") int size);

    PostDetailVO selectDetailWithAuthor(@Param("id") Long id,
                                        @Param("userId") Long userId);

    int incrementViewCount(@Param("id") Long id);
}

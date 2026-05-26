package com.echospace.service;

import com.echospace.dto.CreatePostDTO;
import com.echospace.dto.UpdatePostDTO;
import com.echospace.vo.CursorPageVO;
import com.echospace.vo.PostDetailVO;
import com.echospace.vo.PostItemVO;

public interface PostService {

    Long createPost(CreatePostDTO dto);

    PostDetailVO getPostDetail(Long postId);

    void updatePost(Long postId, UpdatePostDTO dto);

    void deletePost(Long postId);

    CursorPageVO<PostItemVO> listPosts(String cursor, int size, String sort);
}

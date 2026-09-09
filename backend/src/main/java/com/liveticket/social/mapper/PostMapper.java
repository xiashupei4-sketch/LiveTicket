package com.liveticket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liveticket.social.entity.Post;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface PostMapper extends BaseMapper<Post> {

    @Update("UPDATE lt_post SET like_count = like_count + 1 WHERE id = #{postId}")
    int incrementLike(@Param("postId") Long postId);

    @Update("UPDATE lt_post SET like_count = like_count - 1 "
            + "WHERE id = #{postId} AND like_count > 0")
    int decrementLike(@Param("postId") Long postId);
}

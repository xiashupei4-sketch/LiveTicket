package com.liveticket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liveticket.social.entity.PostLike;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface PostLikeMapper extends BaseMapper<PostLike> {

    @Delete("DELETE FROM lt_post_like WHERE post_id = #{postId} AND user_id = #{userId}")
    int deleteLike(@Param("postId") Long postId, @Param("userId") Long userId);
}

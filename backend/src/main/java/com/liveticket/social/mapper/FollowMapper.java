package com.liveticket.social.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.liveticket.social.entity.Follow;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

public interface FollowMapper extends BaseMapper<Follow> {

    @Delete("DELETE FROM lt_follow WHERE user_id = #{userId} AND follow_user_id = #{followUserId}")
    int deleteFollow(@Param("userId") Long userId, @Param("followUserId") Long followUserId);
}

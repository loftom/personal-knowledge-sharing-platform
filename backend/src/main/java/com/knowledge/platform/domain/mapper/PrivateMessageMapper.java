package com.knowledge.platform.domain.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.platform.domain.entity.PrivateMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface PrivateMessageMapper extends BaseMapper<PrivateMessage> {

    @Select("""
            SELECT *
            FROM private_message
            WHERE sender_user_id = #{userId}
               OR receiver_user_id = #{userId}
            ORDER BY created_at DESC, id DESC
            """)
    List<PrivateMessage> selectByUserId(Long userId);

    @Select("""
            SELECT *
            FROM private_message
            WHERE (sender_user_id = #{currentUserId} AND receiver_user_id = #{peerUserId})
               OR (sender_user_id = #{peerUserId} AND receiver_user_id = #{currentUserId})
            ORDER BY created_at ASC, id ASC
            """)
    List<PrivateMessage> selectConversation(Long currentUserId, Long peerUserId);

    @Select("""
            SELECT COUNT(1)
            FROM private_message
            WHERE receiver_user_id = #{userId}
              AND is_read = 0
            """)
    Long countUnreadByUserId(Long userId);
}

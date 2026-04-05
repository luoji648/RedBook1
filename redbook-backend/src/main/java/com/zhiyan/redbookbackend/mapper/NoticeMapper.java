package com.zhiyan.redbookbackend.mapper;

import com.zhiyan.redbookbackend.dto.resp.InteractionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoticeMapper {

    @Select("""
            SELECT type, actor_id AS actorId, note_id AS noteId, event_time AS eventTime FROM (
              SELECT 'LIKE' AS type, nl.user_id AS actor_id, nl.note_id AS note_id, nl.create_time AS event_time
              FROM tb_note_like nl INNER JOIN tb_note n ON n.id = nl.note_id
              WHERE n.user_id = #{ownerId} AND nl.user_id <> n.user_id
              UNION ALL
              SELECT 'COLLECT' AS type, nc.user_id AS actor_id, nc.note_id AS note_id, nc.create_time AS event_time
              FROM tb_note_collect nc INNER JOIN tb_note n ON n.id = nc.note_id
              WHERE n.user_id = #{ownerId} AND nc.user_id <> n.user_id
              UNION ALL
              SELECT 'COMMENT' AS type, c.user_id AS actor_id, c.note_id AS note_id, c.create_time AS event_time
              FROM tb_note_comment c INNER JOIN tb_note n ON n.id = c.note_id
              WHERE n.user_id = #{ownerId} AND c.user_id <> n.user_id AND c.status = 0
              UNION ALL
              SELECT 'SHARE' AS type, sl.creator_user_id AS actor_id, sl.note_id AS note_id, sl.create_time AS event_time
              FROM tb_share_link sl INNER JOIN tb_note n ON n.id = sl.note_id
              WHERE n.user_id = #{ownerId} AND sl.creator_user_id <> n.user_id
            ) t
            ORDER BY event_time DESC
            LIMIT #{offset}, #{limit}
            """)
    List<InteractionRow> listInteractions(@Param("ownerId") long ownerId,
                                          @Param("offset") long offset,
                                          @Param("limit") int limit);

    @Select("""
            SELECT COUNT(1) FROM (
              SELECT nl.id FROM tb_note_like nl INNER JOIN tb_note n ON n.id = nl.note_id
              WHERE n.user_id = #{ownerId} AND nl.user_id <> n.user_id
              UNION ALL
              SELECT nc.id FROM tb_note_collect nc INNER JOIN tb_note n ON n.id = nc.note_id
              WHERE n.user_id = #{ownerId} AND nc.user_id <> n.user_id
              UNION ALL
              SELECT c.id FROM tb_note_comment c INNER JOIN tb_note n ON n.id = c.note_id
              WHERE n.user_id = #{ownerId} AND c.user_id <> n.user_id AND c.status = 0
              UNION ALL
              SELECT sl.short_code FROM tb_share_link sl INNER JOIN tb_note n ON n.id = sl.note_id
              WHERE n.user_id = #{ownerId} AND sl.creator_user_id <> n.user_id
            ) x
            """)
    Long countInteractions(@Param("ownerId") long ownerId);

    @Select("""
            SELECT type, actor_id AS actorId, note_id AS noteId, event_time AS eventTime FROM (
              SELECT 'LIKE' AS type, nl.user_id AS actor_id, nl.note_id AS note_id, nl.create_time AS event_time
              FROM tb_note_like nl INNER JOIN tb_note n ON n.id = nl.note_id
              WHERE n.user_id = #{ownerId} AND nl.user_id <> n.user_id
              UNION ALL
              SELECT 'COLLECT' AS type, nc.user_id AS actor_id, nc.note_id AS note_id, nc.create_time AS event_time
              FROM tb_note_collect nc INNER JOIN tb_note n ON n.id = nc.note_id
              WHERE n.user_id = #{ownerId} AND nc.user_id <> n.user_id
            ) t
            ORDER BY event_time DESC
            LIMIT #{offset}, #{limit}
            """)
    List<InteractionRow> listLikeCollect(@Param("ownerId") long ownerId,
                                           @Param("offset") long offset,
                                           @Param("limit") int limit);

    @Select("""
            SELECT COUNT(1) FROM (
              SELECT nl.id FROM tb_note_like nl INNER JOIN tb_note n ON n.id = nl.note_id
              WHERE n.user_id = #{ownerId} AND nl.user_id <> n.user_id
              UNION ALL
              SELECT nc.id FROM tb_note_collect nc INNER JOIN tb_note n ON n.id = nc.note_id
              WHERE n.user_id = #{ownerId} AND nc.user_id <> n.user_id
            ) x
            """)
    Long countLikeCollect(@Param("ownerId") long ownerId);

    @Select("""
            SELECT type, actor_id AS actorId, note_id AS noteId, event_time AS eventTime FROM (
              SELECT 'COMMENT' AS type, c.user_id AS actor_id, c.note_id AS note_id, c.create_time AS event_time
              FROM tb_note_comment c INNER JOIN tb_note n ON n.id = c.note_id
              WHERE n.user_id = #{ownerId} AND c.user_id <> n.user_id AND c.status = 0
              UNION ALL
              SELECT 'SHARE' AS type, sl.creator_user_id AS actor_id, sl.note_id AS note_id, sl.create_time AS event_time
              FROM tb_share_link sl INNER JOIN tb_note n ON n.id = sl.note_id
              WHERE n.user_id = #{ownerId} AND sl.creator_user_id <> n.user_id
            ) t
            ORDER BY event_time DESC
            LIMIT #{offset}, #{limit}
            """)
    List<InteractionRow> listCommentShare(@Param("ownerId") long ownerId,
                                          @Param("offset") long offset,
                                          @Param("limit") int limit);

    @Select("""
            SELECT COUNT(1) FROM (
              SELECT c.id FROM tb_note_comment c INNER JOIN tb_note n ON n.id = c.note_id
              WHERE n.user_id = #{ownerId} AND c.user_id <> n.user_id AND c.status = 0
              UNION ALL
              SELECT sl.short_code FROM tb_share_link sl INNER JOIN tb_note n ON n.id = sl.note_id
              WHERE n.user_id = #{ownerId} AND sl.creator_user_id <> n.user_id
            ) x
            """)
    Long countCommentShare(@Param("ownerId") long ownerId);

    @Select("""
            SELECT 'FOLLOW' AS type, f.follower_id AS actorId, NULL AS noteId, f.create_time AS eventTime
            FROM tb_user_follow f
            WHERE f.followee_id = #{ownerId}
            ORDER BY f.create_time DESC
            LIMIT #{offset}, #{limit}
            """)
    List<InteractionRow> listFollowNotice(@Param("ownerId") long ownerId,
                                          @Param("offset") long offset,
                                          @Param("limit") int limit);

    @Select("SELECT COUNT(1) FROM tb_user_follow WHERE followee_id = #{ownerId}")
    Long countFollowNotice(@Param("ownerId") long ownerId);
}

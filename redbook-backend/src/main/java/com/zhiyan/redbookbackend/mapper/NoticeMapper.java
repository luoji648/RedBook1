package com.zhiyan.redbookbackend.mapper;

import com.zhiyan.redbookbackend.dto.resp.InteractionRow;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NoticeMapper {

    List<InteractionRow> listInteractions(@Param("ownerId") long ownerId,
                                          @Param("offset") long offset,
                                          @Param("limit") int limit);

    Long countInteractions(@Param("ownerId") long ownerId);

    List<InteractionRow> listLikeCollect(@Param("ownerId") long ownerId,
                                         @Param("offset") long offset,
                                         @Param("limit") int limit);

    Long countLikeCollect(@Param("ownerId") long ownerId);

    List<InteractionRow> listCommentShare(@Param("ownerId") long ownerId,
                                          @Param("offset") long offset,
                                          @Param("limit") int limit);

    Long countCommentShare(@Param("ownerId") long ownerId);

    List<InteractionRow> listFollowNotice(@Param("ownerId") long ownerId,
                                          @Param("offset") long offset,
                                          @Param("limit") int limit);

    Long countFollowNotice(@Param("ownerId") long ownerId);

    Long countUnreadLikeCollect(@Param("ownerId") long ownerId,
                                @Param("since") LocalDateTime since);

    Long countUnreadFollowNotice(@Param("ownerId") long ownerId,
                                 @Param("since") LocalDateTime since);

    Long countUnreadCommentShare(@Param("ownerId") long ownerId,
                                 @Param("since") LocalDateTime since);
}

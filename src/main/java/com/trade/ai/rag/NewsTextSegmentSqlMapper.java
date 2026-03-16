package com.trade.ai.rag;

import com.trade.entity.NewsTextSegment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface NewsTextSegmentSqlMapper {

    int insertBatch(@Param("list") List<NewsTextSegment> list);

    int deleteByNewsId(@Param("newsId") Long newsId);

    Integer countByNewsId(@Param("newsId") Long newsId);

    List<NewsTextSegmentWithNews> selectCandidates(@Param("start") LocalDateTime start,
                                                  @Param("end") LocalDateTime end,
                                                  @Param("limit") Integer limit);

    List<Long> selectRecentNewsIds(@Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  @Param("limit") Integer limit);
}

package com.example.smartcustomerservice.mapper.statistics;

import com.example.smartcustomerservice.domain.vo.AgentWorkloadStatisticsVO;
import com.example.smartcustomerservice.domain.vo.DashboardStatisticsVO;
import com.example.smartcustomerservice.domain.vo.SlaPerformanceStatisticsVO;
import com.example.smartcustomerservice.domain.vo.TicketTrendStatisticsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
// StatisticsMapper 用于封装跨会话、工单和 SLA 表的运营聚合查询。
public interface StatisticsMapper {


    DashboardStatisticsVO selectDashboardStatistics(@Param("dayStart") LocalDateTime dayStart,
                                                     @Param("dayEnd") LocalDateTime dayEnd,
                                                     @Param("now") LocalDateTime now);

    List<AgentWorkloadStatisticsVO> selectAgentWorkloads();

    List<TicketTrendStatisticsVO> selectTicketTrends(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);

    SlaPerformanceStatisticsVO selectSlaPerformance(@Param("now") LocalDateTime now);
}

package com.example.smartcustomerservice.service.statistics;

import com.example.smartcustomerservice.domain.vo.AgentWorkloadStatisticsVO;
import com.example.smartcustomerservice.domain.vo.DashboardStatisticsVO;
import com.example.smartcustomerservice.domain.vo.SlaPerformanceStatisticsVO;
import com.example.smartcustomerservice.domain.vo.TicketTrendStatisticsVO;

import java.util.List;

// StatisticsService 提供运营看板统计能力。
public interface StatisticsService {

    DashboardStatisticsVO getDashboardStatistics();

    List<AgentWorkloadStatisticsVO> listAgentWorkloads();

    List<TicketTrendStatisticsVO> listTicketTrends(Integer days);

    SlaPerformanceStatisticsVO getSlaPerformance();
}

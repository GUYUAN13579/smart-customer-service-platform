package com.example.smartcustomerservice.service.impl.statistics;

import com.example.smartcustomerservice.domain.vo.AgentWorkloadStatisticsVO;
import com.example.smartcustomerservice.domain.vo.DashboardStatisticsVO;
import com.example.smartcustomerservice.domain.vo.SlaPerformanceStatisticsVO;
import com.example.smartcustomerservice.domain.vo.TicketTrendStatisticsVO;
import com.example.smartcustomerservice.mapper.statistics.StatisticsMapper;
import com.example.smartcustomerservice.service.statistics.StatisticsService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

@Service
// StatisticsServiceImpl 负责组装运营看板所需的聚合指标。
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;

    public StatisticsServiceImpl(StatisticsMapper statisticsMapper) {
        this.statisticsMapper = statisticsMapper;
    }

    @Override
    public DashboardStatisticsVO getDashboardStatistics() {
        // TODO: 按当天时间范围查询并返回工单、会话、SLA 和 AI 统计指标。
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        return statisticsMapper.selectDashboardStatistics(dayStart, dayEnd, now);
    }

    @Override
    public List<AgentWorkloadStatisticsVO> listAgentWorkloads() {
        return statisticsMapper.selectAgentWorkloads();
    }

    @Override
    public List<TicketTrendStatisticsVO> listTicketTrends(Integer days) {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(days - 1L);
        Map<LocalDate, TicketTrendStatisticsVO> trendByDate = new HashMap<>();
        for (TicketTrendStatisticsVO trend : statisticsMapper.selectTicketTrends(
                startDate.atStartOfDay(), today.plusDays(1).atStartOfDay())) {
            trendByDate.put(trend.getStatDate(), trend);
        }

        return IntStream.range(0, days)
                .mapToObj(offset -> trendByDate.getOrDefault(startDate.plusDays(offset), emptyTrend(startDate.plusDays(offset))))
                .toList();
    }

    @Override
    public SlaPerformanceStatisticsVO getSlaPerformance() {
        return statisticsMapper.selectSlaPerformance(LocalDateTime.now());
    }

    private TicketTrendStatisticsVO emptyTrend(LocalDate statDate) {
        TicketTrendStatisticsVO trend = new TicketTrendStatisticsVO();
        trend.setStatDate(statDate);
        trend.setCreatedTicketCount(0L);
        trend.setResolvedTicketCount(0L);
        trend.setClosedTicketCount(0L);
        return trend;
    }
}

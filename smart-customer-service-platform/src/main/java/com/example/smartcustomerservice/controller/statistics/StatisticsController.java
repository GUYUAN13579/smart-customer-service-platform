package com.example.smartcustomerservice.controller.statistics;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.domain.vo.AgentWorkloadStatisticsVO;
import com.example.smartcustomerservice.domain.vo.DashboardStatisticsVO;
import com.example.smartcustomerservice.domain.vo.SlaPerformanceStatisticsVO;
import com.example.smartcustomerservice.domain.vo.TicketTrendStatisticsVO;
import com.example.smartcustomerservice.service.statistics.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.List;

@Tag(name = "运营统计")
@Validated
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/statistics")
public class StatisticsController {

    private final StatisticsService statisticsService;

    public StatisticsController(StatisticsService statisticsService) {
        this.statisticsService = statisticsService;
    }

    @Operation(summary = "运营看板统计")
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('statistics:dashboard')")
    public ApiResult<DashboardStatisticsVO> getDashboardStatistics() {
        return ApiResult.success(statisticsService.getDashboardStatistics());
    }

    @Operation(summary = "客服实时负载统计")
    @GetMapping("/agent-workloads")
    @PreAuthorize("hasAuthority('statistics:agent-workload')")
    public ApiResult<List<AgentWorkloadStatisticsVO>> listAgentWorkloads() {
        return ApiResult.success(statisticsService.listAgentWorkloads());
    }

    @Operation(summary = "工单生命周期趋势统计")
    @GetMapping("/ticket-trends")
    @PreAuthorize("hasAuthority('statistics:ticket-trend')")
    public ApiResult<List<TicketTrendStatisticsVO>> listTicketTrends(
            @RequestParam(defaultValue = "7") @Min(value = 1, message = "统计天数最少为1天")
            @Max(value = 90, message = "统计天数最多为90天") Integer days) {
        return ApiResult.success(statisticsService.listTicketTrends(days));
    }

    @Operation(summary = "SLA达标表现统计")
    @GetMapping("/sla-performance")
    @PreAuthorize("hasAuthority('statistics:sla-performance')")
    public ApiResult<SlaPerformanceStatisticsVO> getSlaPerformance() {
        return ApiResult.success(statisticsService.getSlaPerformance());
    }
}

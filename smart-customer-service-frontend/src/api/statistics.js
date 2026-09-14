// statistics.js 集中封装当前后端模块的前端请求。
import { request, toQuery } from './request';

export const statisticsApi = {
  dashboard(params) {
    return request(`/api/v1/statistics/dashboard${toQuery(params)}`);
  },
  agentWorkloads() {
    return request('/api/v1/statistics/agent-workloads');
  },
  ticketTrends(days = 7) {
    return request(`/api/v1/statistics/ticket-trends${toQuery({ days })}`);
  },
  slaPerformance() {
    return request('/api/v1/statistics/sla-performance');
  }
};

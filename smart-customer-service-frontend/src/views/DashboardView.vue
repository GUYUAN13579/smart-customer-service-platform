<template>
  <section class="page-stack">
    <div class="ops-hero">
      <div>
        <p class="eyebrow">Today Overview</p>
        <h2>客服运营总览</h2>
        <p>查看工单流转、客服负载和 SLA 表现，快速定位需要处理的运营风险。</p>
      </div>
      <button class="primary-button" :disabled="loading" @click="loadData">{{ loading ? '加载中' : '刷新看板' }}</button>
    </div>

    <div class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </div>

    <div v-if="errorMessage" class="inline-error">{{ errorMessage }}</div>

    <div class="panel-grid statistics-top-grid">
      <section class="panel trend-panel">
        <div class="panel-title">
          <div>
            <p class="eyebrow">Ticket Trend</p>
            <h2>近 7 天工单趋势</h2>
          </div>
          <span class="panel-note">新增 / 解决 / 关闭</span>
        </div>
        <div v-if="trends.length" class="trend-chart" aria-label="近七天工单趋势">
          <div v-for="item in trends" :key="item.statDate" class="trend-column">
            <div class="trend-bars">
              <span class="trend-bar created" :style="{ height: barHeight(item.createdTicketCount) }" :title="`新增 ${item.createdTicketCount}`"></span>
              <span class="trend-bar resolved" :style="{ height: barHeight(item.resolvedTicketCount) }" :title="`解决 ${item.resolvedTicketCount}`"></span>
              <span class="trend-bar closed" :style="{ height: barHeight(item.closedTicketCount) }" :title="`关闭 ${item.closedTicketCount}`"></span>
            </div>
            <strong>{{ formatDay(item.statDate) }}</strong>
          </div>
        </div>
        <div v-else class="empty-state compact">暂无趋势数据</div>
        <div class="chart-legend">
          <span><i class="created"></i>新增</span>
          <span><i class="resolved"></i>解决</span>
          <span><i class="closed"></i>关闭</span>
        </div>
      </section>

      <section class="panel sla-performance-panel">
        <div class="panel-title">
          <div>
            <p class="eyebrow">SLA Performance</p>
            <h2>SLA 达标表现</h2>
          </div>
        </div>
        <div class="sla-score-list">
          <div class="sla-score-row">
            <div>
              <strong>首次响应</strong>
              <span>考核 {{ slaPerformance.firstResponseEvaluatedCount }} 单，违约 {{ slaPerformance.firstResponseBreachedCount }} 单</span>
            </div>
            <b>{{ percentage(slaPerformance.firstResponseComplianceRate) }}</b>
          </div>
          <div class="progress-track"><i :style="{ width: percentage(slaPerformance.firstResponseComplianceRate) }"></i></div>
          <div class="sla-score-row">
            <div>
              <strong>解决时效</strong>
              <span>考核 {{ slaPerformance.resolveEvaluatedCount }} 单，违约 {{ slaPerformance.resolveBreachedCount }} 单</span>
            </div>
            <b>{{ percentage(slaPerformance.resolveComplianceRate) }}</b>
          </div>
          <div class="progress-track resolve"><i :style="{ width: percentage(slaPerformance.resolveComplianceRate) }"></i></div>
        </div>
      </section>
    </div>

    <section class="panel workload-panel">
      <div class="panel-title">
        <div>
          <p class="eyebrow">Agent Workload</p>
          <h2>客服实时负载</h2>
        </div>
        <span class="panel-note">按技能组展示</span>
      </div>
      <div v-if="workloads.length" class="workload-table-wrap">
        <table class="workload-table">
          <thead>
            <tr>
              <th>客服</th>
              <th>技能组</th>
              <th>已分配</th>
              <th>处理中</th>
              <th>负载</th>
              <th>派单状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in workloads" :key="item.skillGroupMemberId">
              <td><strong>{{ item.realName || item.username }}</strong><span>{{ item.username }}</span></td>
              <td>{{ item.skillGroupName }}</td>
              <td>{{ item.assignedTicketCount }}</td>
              <td>{{ item.processingTicketCount }}</td>
              <td>
                <div class="load-cell">
                  <div class="mini-progress"><i :class="{ danger: Number(item.workloadRate) >= 1 }" :style="{ width: percentage(item.workloadRate) }"></i></div>
                  <span>{{ item.activeTicketCount }} / {{ item.maxActiveTickets }}</span>
                </div>
              </td>
              <td><span class="status-pill" :class="item.availableForAssign ? 'success' : 'muted'">{{ item.availableForAssign ? '可派单' : '已满载 / 停用' }}</span></td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-else class="empty-state compact">暂无可展示的客服技能组成员</div>
    </section>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { statisticsApi } from '../api/statistics';

const loading = ref(false);
const errorMessage = ref('');
const dashboard = ref({});
const trends = ref([]);
const workloads = ref([]);
const slaPerformance = ref({
  firstResponseEvaluatedCount: 0,
  firstResponseBreachedCount: 0,
  firstResponseComplianceRate: 0,
  resolveEvaluatedCount: 0,
  resolveBreachedCount: 0,
  resolveComplianceRate: 0
});

const metrics = computed(() => [
  { label: '工单总量', value: dashboard.value.ticketCount ?? 0, hint: `今日新增 ${dashboard.value.todayTicketCount ?? 0} 单` },
  { label: '处理中', value: dashboard.value.processingTicketCount ?? 0, hint: `待分配 ${dashboard.value.waitingAssignTicketCount ?? 0} 单` },
  { label: 'SLA 风险', value: dashboard.value.riskTicketCount ?? 0, hint: `已超时 ${dashboard.value.overdueTicketCount ?? 0} 单` },
  { label: '平均首响', value: `${dashboard.value.avgFirstResponseMinutes ?? 0}m`, hint: `AI 覆盖 ${(Number(dashboard.value.aiResolveRate ?? 0) * 100).toFixed(0)}%` }
]);

const trendMax = computed(() => Math.max(1, ...trends.value.flatMap((item) => [
  Number(item.createdTicketCount || 0),
  Number(item.resolvedTicketCount || 0),
  Number(item.closedTicketCount || 0)
])));

function percentage(value) {
  return `${Math.min(100, Math.max(0, Math.round(Number(value || 0) * 100)))}%`;
}

function barHeight(value) {
  return `${Math.max(4, Math.round((Number(value || 0) / trendMax.value) * 132))}px`;
}

function formatDay(value) {
  return value ? String(value).slice(5).replace('-', '/') : '-';
}

async function loadData() {
  loading.value = true;
  errorMessage.value = '';
  const [dashboardResult, trendResult, workloadResult, slaResult] = await Promise.allSettled([
    statisticsApi.dashboard(),
    statisticsApi.ticketTrends(7),
    statisticsApi.agentWorkloads(),
    statisticsApi.slaPerformance()
  ]);

  if (dashboardResult.status === 'fulfilled') dashboard.value = dashboardResult.value || {};
  if (trendResult.status === 'fulfilled') trends.value = trendResult.value || [];
  if (workloadResult.status === 'fulfilled') workloads.value = workloadResult.value || [];
  if (slaResult.status === 'fulfilled') slaPerformance.value = { ...slaPerformance.value, ...slaResult.value };

  const failedSections = [
    ['看板概览', dashboardResult],
    ['工单趋势', trendResult],
    ['客服负载', workloadResult],
    ['SLA 表现', slaResult]
  ].filter(([, result]) => result.status === 'rejected');
  if (failedSections.length) {
    const labels = failedSections.map(([label]) => label).join('、');
    const reasons = failedSections
      .map(([, result]) => result.reason?.message)
      .filter(Boolean)
      .join('；');
    errorMessage.value = `${labels}加载失败${reasons ? `：${reasons}` : '。请检查接口权限或后端日志。'}`;
  }
  loading.value = false;
}

onMounted(loadData);
</script>

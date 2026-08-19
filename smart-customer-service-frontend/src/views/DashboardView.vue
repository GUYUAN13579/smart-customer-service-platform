<template>
  <section class="page-stack">
    <div class="ops-hero">
      <div>
        <p class="eyebrow">Today Overview</p>
        <h2>客服运营总览</h2>
        <p>聚合工单压力、SLA 风险、AI 处理效果和待办事项，用于模拟真实客服主管的日常看板。</p>
      </div>
      <button class="primary-button" @click="loadData">刷新看板</button>
    </div>

    <div class="metric-grid">
      <div v-for="item in metrics" :key="item.label" class="metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </div>
    </div>

    <div class="panel-grid">
      <div class="panel">
        <div class="panel-title">
          <div>
            <h2>工单处理链路</h2>
            <p>从客户会话到自动派单的主流程。</p>
          </div>
        </div>
        <div class="timeline">
          <div v-for="item in timeline" :key="item.title">
            <span></span>
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.desc }}</p>
            </div>
          </div>
        </div>
      </div>
      <div class="panel">
        <h2>待关注事项</h2>
        <ul class="task-list">
          <li v-for="item in focusItems" :key="item.title">
            <strong>{{ item.title }}</strong>
            <span>{{ item.desc }}</span>
          </li>
        </ul>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { statisticsApi } from '../api/statistics';

const metrics = ref([
  { label: '待处理工单', value: '42', hint: '含 6 个 P1 高优先级' },
  { label: '今日新增会话', value: '168', hint: 'WEB 与 APP 渠道为主' },
  { label: '平均首响', value: '8.5m', hint: 'SLA 当前健康' },
  { label: 'AI 草稿采纳率', value: '37%', hint: '知识库命中后提升明显' }
]);

const timeline = [
  { title: '客户进入会话', desc: '客户通过 WEB、APP、微信、电话或邮件进入统一会话中心。' },
  { title: 'AI 辅助判断', desc: 'AI 读取上下文和知识库，生成建议回复或工单草稿。' },
  { title: '人工确认建单', desc: '坐席确认客户、分类、优先级和问题描述后创建工单。' },
  { title: '规则派单与 SLA', desc: '系统按分类、优先级和技能组规则分配处理人并持续监控超时风险。' }
];

const focusItems = [
  { title: 'P1 工单', desc: '优先查看高优先级账号、支付类问题。' },
  { title: '低置信度会话', desc: 'AI 无法确认意图时建议转人工跟进。' },
  { title: '派单规则', desc: '规则命中异常时检查分类、优先级和技能组配置。' },
  { title: '知识库补全', desc: '把重复问题沉淀为知识文章，提升 AI 草稿质量。' }
];

async function loadData() {
  try {
    const data = await statisticsApi.dashboard({});
    metrics.value = [
      { label: '工单总量', value: data.ticketCount ?? 0, hint: '查询周期内' },
      { label: '已关闭', value: data.closedCount ?? 0, hint: '处理完成' },
      { label: '平均首响', value: `${data.avgFirstResponseMinutes ?? 0}m`, hint: '响应效率' },
      { label: 'AI 解决率', value: `${Math.round((data.aiResolveRate ?? 0) * 100)}%`, hint: '自动化效果' }
    ];
  } catch {
    // 后端统计接口未完成时保留运营演示数据。
  }
}

onMounted(loadData);
</script>

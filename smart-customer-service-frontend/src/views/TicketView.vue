<template>
  <section class="page-stack">
    <div class="section-header">
      <div>
        <p class="eyebrow">Ticket Desk</p>
        <h2>工单中心</h2>
        <p>统一处理工单查询、创建、派单和关闭，模拟真实客服坐席的核心工作台。</p>
      </div>
      <button class="primary-button" @click="openCreate">创建工单</button>
    </div>

    <div class="workbench-grid">
      <div class="panel">
        <div class="filter-bar">
          <label>关键词<input v-model.trim="query.keyword" placeholder="标题 / 工单编号" @keyup.enter="loadTickets" /></label>
          <label>状态
            <AppSelect v-model="query.status" :options="statusFilterOptions" @change="loadTickets" />
          </label>
          <label>优先级
            <AppSelect v-model="query.priority" :options="priorityFilterOptions" @change="loadTickets" />
          </label>
          <div class="filter-actions">
            <button class="ghost-button" @click="loadTickets">查询</button>
          </div>
        </div>

        <div class="ticket-list">
          <article v-for="ticket in tickets" :key="ticket.id" class="ticket-card" :class="{ active: selected?.id === ticket.id }" @click="selectTicket(ticket)">
            <div>
              <strong>{{ ticket.title }}</strong>
              <small>{{ ticket.ticketNo || `#${ticket.id}` }} · 客户 {{ ticket.customerId }}</small>
            </div>
            <div class="ticket-meta">
              <span class="status-pill" :class="statusClass(ticket.status)">{{ ticket.status || 'OPEN' }}</span>
              <span class="priority-pill">{{ ticket.priority || '-' }}</span>
            </div>
          </article>
          <div v-if="!loading && !tickets.length" class="empty-state">
            <strong>暂无工单</strong>
            <span>后端工单查询接口完成后，这里会显示真实分页列表。</span>
          </div>
          <div v-if="loading" class="empty-state">正在加载工单...</div>
        </div>
      </div>

      <aside class="panel detail-panel">
        <template v-if="selected">
          <p class="eyebrow">Ticket Detail</p>
          <h3>{{ selected.title }}</h3>
          <p class="muted">{{ selected.content || selected.description || '暂无问题描述' }}</p>
          <dl class="detail-list">
            <div><dt>工单ID</dt><dd>{{ selected.id }}</dd></div>
            <div><dt>客户ID</dt><dd>{{ selected.customerId }}</dd></div>
            <div><dt>会话ID</dt><dd>{{ selected.sessionId || '-' }}</dd></div>
            <div><dt>分类</dt><dd>{{ selected.category || '-' }}</dd></div>
            <div><dt>渠道</dt><dd>{{ selected.sourceChannel || '-' }}</dd></div>
            <div><dt>处理人</dt><dd>{{ selected.assigneeId || '-' }}</dd></div>
          </dl>
          <div class="inline-form">
            <input v-model.number="assignForm.assigneeId" type="number" placeholder="处理人用户ID" />
            <button class="ghost-button" @click="assignTicket">派单</button>
          </div>
          <div class="inline-form">
            <input v-model.trim="closeForm.resolution" placeholder="关闭说明" />
            <button class="ghost-button" @click="closeTicket">关闭</button>
          </div>
          <p v-if="detailError" class="error-text">{{ detailError }}</p>
        </template>
        <div v-else class="empty-state">
          <strong>选择一个工单查看详情</strong>
          <span>可进行派单、关闭等后续操作。</span>
        </div>
      </aside>
    </div>

    <div v-if="dialogVisible" class="modal-backdrop" @click.self="dialogVisible = false">
      <form class="modal large-modal" @submit.prevent="createTicket">
        <div class="modal-head">
          <div>
            <p class="eyebrow">New Ticket</p>
            <h3>创建工单</h3>
          </div>
          <button class="icon-button" type="button" @click="dialogVisible = false">×</button>
        </div>
        <div class="form-grid">
          <label>客户ID<input v-model.number="form.customerId" required type="number" /></label>
          <label>会话ID<input v-model.number="form.sessionId" type="number" /></label>
          <label>来源渠道
            <AppSelect v-model="form.sourceChannel" :options="channelOptions" />
          </label>
          <label>分类<input v-model.trim="form.category" placeholder="ACCOUNT" /></label>
          <label>优先级
            <AppSelect v-model="form.priority" :options="priorityOptions" />
          </label>
          <label>标题<input v-model.trim="form.title" required /></label>
        </div>
        <label>问题内容<textarea v-model.trim="form.content" required rows="5"></textarea></label>
        <p v-if="error" class="error-text">{{ error }}</p>
        <div class="modal-actions">
          <button class="ghost-button" type="button" @click="dialogVisible = false">取消</button>
          <button class="primary-button" :disabled="saving">{{ saving ? '创建中...' : '创建工单' }}</button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppSelect from '../components/AppSelect.vue';
import { ticketApi } from '../api/tickets';

const tickets = ref([]);
const selected = ref(null);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const error = ref('');
const detailError = ref('');
const query = reactive({ page: 1, size: 20, keyword: '', status: '', priority: '' });
const statusFilterOptions = [
  { label: '全部状态', value: '' },
  { label: '待处理 OPEN', value: 'OPEN' },
  { label: '已派单 ASSIGNED', value: 'ASSIGNED' },
  { label: '处理中 PROCESSING', value: 'PROCESSING' },
  { label: '已关闭 CLOSED', value: 'CLOSED' }
];
const priorityFilterOptions = [
  { label: '全部优先级', value: '' },
  { label: 'P1 高优先级', value: 'P1' },
  { label: 'P2 普通优先级', value: 'P2' },
  { label: 'P3 低优先级', value: 'P3' }
];
const priorityOptions = priorityFilterOptions.filter((option) => option.value !== '');
const channelOptions = ['WEB', 'APP', 'WECHAT', 'PHONE', 'EMAIL'];
const assignForm = reactive({ assigneeId: null });
const closeForm = reactive({ resolution: '' });
const form = reactive({
  customerId: null,
  sessionId: null,
  title: '',
  content: '',
  category: 'ACCOUNT',
  priority: 'P2',
  sourceChannel: 'WEB'
});

function openCreate() {
  Object.assign(form, { customerId: null, sessionId: null, title: '', content: '', category: 'ACCOUNT', priority: 'P2', sourceChannel: 'WEB' });
  error.value = '';
  dialogVisible.value = true;
}

async function loadTickets() {
  loading.value = true;
  try {
    const data = await ticketApi.page(query);
    tickets.value = data?.records || [];
    selected.value = tickets.value[0] || null;
  } catch {
    tickets.value = [];
  } finally {
    loading.value = false;
  }
}

async function selectTicket(ticket) {
  detailError.value = '';
  try {
    selected.value = await ticketApi.detail(ticket.id);
  } catch {
    selected.value = ticket;
  }
}

async function createTicket() {
  saving.value = true;
  error.value = '';
  try {
    const created = await ticketApi.create(form);
    dialogVisible.value = false;
    await loadTickets();
    selected.value = created || selected.value;
  } catch (err) {
    error.value = err.message || '创建失败';
  } finally {
    saving.value = false;
  }
}

async function assignTicket() {
  if (!selected.value) return;
  detailError.value = '';
  try {
    selected.value = await ticketApi.assign(selected.value.id, assignForm);
    await loadTickets();
  } catch (err) {
    detailError.value = err.message || '派单失败';
  }
}

async function closeTicket() {
  if (!selected.value) return;
  detailError.value = '';
  try {
    selected.value = await ticketApi.close(selected.value.id, closeForm);
    await loadTickets();
  } catch (err) {
    detailError.value = err.message || '关闭失败';
  }
}

function statusClass(status) {
  return {
    off: status === 'CLOSED',
    vip: status === 'PROCESSING',
    enterprise: status === 'ASSIGNED'
  };
}

onMounted(loadTickets);
</script>

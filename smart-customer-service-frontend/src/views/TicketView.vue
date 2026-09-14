<template>
  <section class="page-stack ticket-desk-page">
    <div class="section-header">
      <div>
        <p class="eyebrow">Ticket Operations</p>
        <h2>工单中心</h2>
        <p>围绕待审核、待派单和我的工单组织处理流程，承接 AI 转人工后的正式工单链路。</p>
      </div>
      <button class="ghost-button" @click="reloadCurrentQueue">刷新队列</button>
    </div>

    <div class="ticket-stage-tabs panel">
      <button
        v-for="item in queueTabs"
        :key="item.value"
        :class="['stage-tab', activeQueue === item.value ? 'active' : '']"
        @click="switchQueue(item.value)"
      >
        <span>{{ item.label }}</span>
        <small>{{ item.description }}</small>
      </button>
    </div>

    <div class="workbench-grid ticket-workbench">
      <div class="panel">
        <div class="panel-title compact-title">
          <div>
            <p class="eyebrow">{{ currentQueueMeta.eyebrow }}</p>
            <h3>{{ currentQueueMeta.title }}</h3>
          </div>
          <span class="status-pill">{{ page.total }} 条</span>
        </div>

        <div class="filter-bar ticket-filter-bar">
          <label>关键词<input v-model.trim="query.keyword" placeholder="工单编号 / 标题 / 客户诉求" @keyup.enter="searchTickets" /></label>
          <label>优先级<AppSelect v-model="query.priority" :options="priorityFilterOptions" @change="searchTickets" /></label>
          <label>分类<input v-model.trim="query.category" placeholder="例如 ORDER_OPERATION" @keyup.enter="searchTickets" /></label>
          <label>客户ID<input v-model.number="query.customerId" type="number" placeholder="客户ID" @keyup.enter="searchTickets" /></label>
          <div class="filter-actions">
            <button class="ghost-button" @click="resetFilters">重置</button>
            <button class="primary-button" @click="searchTickets">查询</button>
          </div>
        </div>

        <div class="ticket-list enterprise-ticket-list">
          <article
            v-for="ticket in tickets"
            :key="ticket.id"
            :class="['ticket-card enterprise-ticket-card', selected?.id === ticket.id ? 'active' : '']"
            @click="selectTicket(ticket)"
          >
            <div class="ticket-card-main">
              <div>
                <strong>{{ ticket.title || '未命名工单' }}</strong>
                <small>{{ ticket.ticketNo || `#${ticket.id}` }} · 客户 {{ ticket.customerId || '-' }} · 会话 {{ ticket.sessionId || '-' }}</small>
              </div>
              <p>{{ ticket.aiSummary || ticket.content || ticket.originalContent || '暂无摘要' }}</p>
            </div>
            <div class="ticket-card-side">
              <span class="status-pill" :class="statusClass(ticket.status)">{{ statusLabel(ticket.status) }}</span>
              <span class="priority-pill">{{ ticket.priority || 'P3' }}</span>
              <small>{{ formatTime(ticket.updatedAt || ticket.createdAt) }}</small>
            </div>
          </article>

          <div v-if="!loading && !tickets.length" class="empty-state">
            <strong>{{ currentQueueMeta.emptyTitle }}</strong>
            <span>{{ currentQueueMeta.emptyText }}</span>
          </div>
          <div v-if="loading" class="empty-state">
            <strong>正在加载工单...</strong>
          </div>
        </div>

        <div class="pager">
          <button class="ghost-button" :disabled="query.page <= 1" @click="changePage(query.page - 1)">上一页</button>
          <span>第 {{ query.page }} 页 / 共 {{ totalPages }} 页</span>
          <button class="ghost-button" :disabled="query.page >= totalPages" @click="changePage(query.page + 1)">下一页</button>
        </div>
      </div>

      <aside class="panel detail-panel ticket-detail-panel">
        <template v-if="selected">
          <div class="panel-title compact-title">
            <div>
              <p class="eyebrow">Ticket Detail</p>
              <h3>{{ selected.title || '工单详情' }}</h3>
              <p>{{ selected.ticketNo || `#${selected.id}` }}</p>
            </div>
            <span class="status-pill" :class="statusClass(selected.status)">{{ statusLabel(selected.status) }}</span>
          </div>

          <dl class="detail-list">
            <div><dt>工单ID</dt><dd>{{ selected.id }}</dd></div>
            <div><dt>客户ID</dt><dd>{{ selected.customerId || '-' }}</dd></div>
            <div><dt>会话ID</dt><dd>{{ selected.sessionId || '-' }}</dd></div>
            <div><dt>分类</dt><dd>{{ selected.category || 'UNKNOWN' }}</dd></div>
            <div><dt>优先级</dt><dd>{{ selected.priority || 'P3' }}</dd></div>
            <div><dt>渠道</dt><dd>{{ selected.sourceChannel || '-' }}</dd></div>
            <div><dt>处理人</dt><dd>{{ selected.assigneeId || '未分配' }}</dd></div>
            <div><dt>审核状态</dt><dd>{{ selected.reviewStatus || '-' }}</dd></div>
          </dl>

          <div class="ticket-section">
            <span>用户原始诉求</span>
            <p>{{ selected.originalContent || '-' }}</p>
          </div>
          <div class="ticket-section">
            <span>AI 标准化总结</span>
            <p>{{ selected.aiSummary || '-' }}</p>
          </div>
          <div class="ticket-section">
            <span>建议处理动作</span>
            <p>{{ selected.suggestedAction || '-' }}</p>
          </div>

          <form v-if="activeQueue === 'pendingReview'" class="ticket-action-panel" @submit.prevent="submitReview('APPROVED')">
            <p class="eyebrow">Review</p>
            <label>标题<input v-model.trim="reviewForm.title" /></label>
            <label>工单内容<textarea v-model.trim="reviewForm.content" rows="4"></textarea></label>
            <label>AI 总结<textarea v-model.trim="reviewForm.aiSummary" rows="4"></textarea></label>
            <div class="form-grid compact-grid">
              <label>分类<input v-model.trim="reviewForm.category" /></label>
              <label>优先级<AppSelect v-model="reviewForm.priority" :options="priorityOptions" /></label>
            </div>
            <label>建议动作<textarea v-model.trim="reviewForm.suggestedAction" rows="3"></textarea></label>
            <label>审核备注<input v-model.trim="reviewForm.reviewRemark" placeholder="驳回时必填" /></label>
            <div class="modal-actions">
              <button class="danger-button" type="button" :disabled="actionLoading" @click="submitReview('REJECTED')">驳回</button>
              <button class="primary-button" :disabled="actionLoading">{{ actionLoading ? '处理中...' : '审核通过' }}</button>
            </div>
          </form>

          <form v-if="activeQueue === 'waitingAssign'" class="ticket-action-panel" @submit.prevent="assignSelectedTicket">
            <p class="eyebrow">Assignment</p>
            <label>客服ID<input v-model.number="assignForm.assigneeId" type="number" placeholder="输入客服用户ID" required /></label>
            <label>技能组ID<input v-model.number="assignForm.skillGroupId" type="number" placeholder="可选" /></label>
            <label>派单备注<input v-model.trim="assignForm.assignRemark" placeholder="可选" /></label>
            <div class="modal-actions">
              <button class="ghost-button" type="button" :disabled="actionLoading || previewLoading" @click="previewAutoAssign">
                {{ previewLoading ? '计算中...' : '自动派单预览' }}
              </button>
              <button class="primary-button" :disabled="actionLoading">{{ actionLoading ? '派单中...' : '确认派单' }}</button>
            </div>
            <div v-if="autoAssignPreview" class="auto-assign-preview">
              <strong>{{ autoAssignPreview.unavailableReason ? '暂不可自动派单' : '自动派单建议' }}</strong>
              <p v-if="autoAssignPreview.unavailableReason">{{ autoAssignPreview.unavailableReason }}</p>
              <template v-else>
                <p>规则：{{ autoAssignPreview.assignmentRuleName }} · 技能组：{{ autoAssignPreview.skillGroupName }}</p>
                <p>推荐客服：{{ autoAssignPreview.selectedAgentName || `用户 #${autoAssignPreview.selectedAgentId}` }}</p>
                <div class="candidate-list"><span v-for="candidate in autoAssignPreview.candidates || []" :key="candidate.memberId">{{ candidate.realName || candidate.username || `用户 #${candidate.userId}` }}：{{ candidate.activeTicketCount || 0 }}/{{ candidate.maxActiveTickets }}</span></div>
                <button class="primary-button" type="button" :disabled="actionLoading" @click="autoAssignSelectedTicket">确认自动派单</button>
              </template>
            </div>
          </form>

          <div v-if="activeQueue === 'my'" class="ticket-action-panel">
            <p class="eyebrow">Handling</p>
            <strong>当前工单处理</strong>
            <span class="muted">客服可以从这里开始处理、补充记录、标记解决或关闭工单。</span>

            <label v-if="selected.status === 'ASSIGNED'">
              开始处理说明
              <input v-model.trim="startForm.remark" placeholder="可选，例如：已开始核查客户反馈" />
            </label>
            <button
              v-if="selected.status === 'ASSIGNED'"
              class="primary-button"
              :disabled="actionLoading"
              @click="startSelectedTicket"
            >
              {{ actionLoading ? '处理中...' : '开始处理' }}
            </button>

            <form v-if="canAddProcessRecord" class="inner-action-form" @submit.prevent="addProcessRecord">
              <div class="form-grid compact-grid">
                <label>记录类型<AppSelect v-model="recordForm.recordType" :options="recordTypeOptions" /></label>
                <label>客户可见<AppSelect v-model="recordForm.visibleToCustomer" :options="visibleOptions" /></label>
              </div>
              <label>处理内容<textarea v-model.trim="recordForm.content" rows="3" placeholder="例如：已联系客户确认订单编号，等待财务审核。"></textarea></label>
              <button class="ghost-button" :disabled="actionLoading || !recordForm.content">
                {{ actionLoading ? '保存中...' : '添加处理记录' }}
              </button>
            </form>

            <form v-if="selected.status === 'PROCESSING'" class="inner-action-form" @submit.prevent="resolveSelectedTicket">
              <label>解决方案<textarea v-model.trim="resolveForm.solution" rows="3" placeholder="填写最终解决方案"></textarea></label>
              <label>客户可见<AppSelect v-model="resolveForm.visibleToCustomer" :options="visibleOptions" /></label>
              <button class="primary-button" :disabled="actionLoading || !resolveForm.solution">
                {{ actionLoading ? '提交中...' : '标记已解决' }}
              </button>
            </form>

            <form v-if="canCloseTicket" class="inner-action-form" @submit.prevent="closeSelectedTicket">
              <label>关闭原因<input v-model.trim="closeForm.closeReason" placeholder="例如：客户问题已解决并确认关闭" /></label>
              <button class="danger-button" :disabled="actionLoading || !closeForm.closeReason">
                {{ actionLoading ? '关闭中...' : '关闭工单' }}
              </button>
            </form>
          </div>

          <div class="ticket-history-grid">
            <section class="ticket-section history-section">
              <div class="history-title">
                <span>处理记录</span>
                <button class="ghost-button mini-button" :disabled="historyLoading" @click="loadTicketHistories">刷新</button>
              </div>
              <div v-if="processRecords.length" class="timeline-list">
                <article v-for="item in processRecords" :key="item.id" class="timeline-item">
                  <strong>{{ processRecordLabel(item.recordType) }}</strong>
                  <p>{{ item.content }}</p>
                  <small>操作人 {{ item.operatorId || '-' }} · {{ formatTime(item.createdAt) }} · {{ item.visibleToCustomer === 1 ? '客户可见' : '内部记录' }}</small>
                </article>
              </div>
              <p v-else>{{ historyLoading ? '正在加载处理记录...' : '暂无处理记录' }}</p>
            </section>

            <section class="ticket-section history-section">
              <div class="history-title">
                <span>操作日志</span>
                <button class="ghost-button mini-button" :disabled="historyLoading" @click="loadTicketHistories">刷新</button>
              </div>
              <div v-if="operationLogs.length" class="timeline-list">
                <article v-for="item in operationLogs" :key="item.id" class="timeline-item">
                  <strong>{{ operationLabel(item.operationType) }}</strong>
                  <p>{{ item.operationContent || '-' }}</p>
                  <small>{{ item.fromStatus || '空' }} -> {{ item.toStatus || '-' }} · 操作人 {{ item.operatorId || '-' }} · {{ formatTime(item.createdAt) }}</small>
                </article>
              </div>
              <p v-else>{{ historyLoading ? '正在加载操作日志...' : '暂无操作日志' }}</p>
            </section>
          </div>

          <p v-if="notice" class="success-text">{{ notice }}</p>
          <p v-if="detailError" class="error-text">{{ detailError }}</p>
        </template>
        <div v-else class="empty-state">
          <strong>选择一张工单</strong>
          <span>详情、审核和派单操作会显示在这里。</span>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
// TicketView.vue 渲染对应的业务工作台页面。
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import AppSelect from '../components/AppSelect.vue';
import { ticketApi } from '../api/tickets';
import { authStore } from '../stores/authStore';

const route = useRoute();

const queueTabs = [
  { value: 'pendingReview', label: '待审核', description: 'AI 生成后等待主管确认', eyebrow: 'Pending Review', title: '待审核工单', emptyTitle: '暂无待审核工单', emptyText: '用户转人工后生成的工单会进入这里。' },
  { value: 'waitingAssign', label: '待派单', description: '审核通过后等待分配客服', eyebrow: 'Waiting Assignment', title: '待派单工单', emptyTitle: '暂无待派单工单', emptyText: '审核通过的工单会进入这里。' },
  { value: 'my', label: '我的工单', description: '当前客服需要处理的工单', eyebrow: 'My Tickets', title: '我的工单', emptyTitle: '暂无我的工单', emptyText: '分配给当前账号的工单会显示在这里。' }
];

const activeQueue = ref('pendingReview');
const tickets = ref([]);
const selected = ref(null);
const loading = ref(false);
const actionLoading = ref(false);
const previewLoading = ref(false);
const historyLoading = ref(false);
const detailError = ref('');
const notice = ref('');
const processRecords = ref([]);
const operationLogs = ref([]);
const page = reactive({ total: 0 });
const query = reactive({ page: 1, size: 10, keyword: '', priority: '', category: '', customerId: null, sessionId: null });
const reviewForm = reactive({ title: '', content: '', aiSummary: '', category: '', priority: 'P3', suggestedAction: '', reviewRemark: '' });
const assignForm = reactive({ assigneeId: null, skillGroupId: null, assignRemark: '' });
const startForm = reactive({ remark: '' });
const recordForm = reactive({ recordType: 'NOTE', content: '', visibleToCustomer: 0 });
const resolveForm = reactive({ solution: '', visibleToCustomer: 1 });
const closeForm = reactive({ closeReason: '' });
const autoAssignPreview = ref(null);

const priorityFilterOptions = [
  { label: '全部优先级', value: '' },
  { label: 'P1 紧急', value: 'P1' },
  { label: 'P2 高', value: 'P2' },
  { label: 'P3 普通', value: 'P3' },
  { label: 'P4 低', value: 'P4' }
];
const priorityOptions = priorityFilterOptions.filter((item) => item.value);
const recordTypeOptions = [
  { label: '备注', value: 'NOTE' },
  { label: '联系客户', value: 'CONTACT_CUSTOMER' },
  { label: '处理中', value: 'PROCESS' },
  { label: '等待中', value: 'WAITING' },
  { label: '其他', value: 'OTHER' }
];
const visibleOptions = [
  { label: '内部记录', value: 0 },
  { label: '客户可见', value: 1 }
];
const currentQueueMeta = computed(() => queueTabs.find((item) => item.value === activeQueue.value) || queueTabs[0]);
const totalPages = computed(() => Math.max(1, Math.ceil(page.total / query.size)));
const currentUser = computed(() => authStore.getUser() || {});
const isManager = computed(() => {
  const roleCodes = currentUser.value.roleCodes || [];
  return roleCodes.includes('ADMIN') || roleCodes.includes('SUPERVISOR');
});
const canAddProcessRecord = computed(() => selected.value && ['ASSIGNED', 'PROCESSING'].includes(selected.value.status));
const canCloseTicket = computed(() => {
  if (!selected.value) return false;
  if (isManager.value) return ['ASSIGNED', 'PROCESSING', 'RESOLVED'].includes(selected.value.status);
  return selected.value.status === 'RESOLVED';
});

function switchQueue(queue) {
  activeQueue.value = queue;
  selected.value = null;
  notice.value = '';
  detailError.value = '';
  autoAssignPreview.value = null;
  query.page = 1;
  loadTickets();
}

async function loadTickets() {
  loading.value = true;
  detailError.value = '';
  notice.value = '';
  const selectedId = selected.value?.id;
  try {
    const params = { ...query };
    const data = activeQueue.value === 'pendingReview'
      ? await ticketApi.pendingReview(params)
      : activeQueue.value === 'waitingAssign'
        ? await ticketApi.waitingAssign(params)
        : await ticketApi.my(params);
    tickets.value = data?.records || [];
    page.total = data?.total || 0;
    if (tickets.value.length) {
      const nextSelected = tickets.value.find((item) => item.id === selectedId) || tickets.value[0];
      await selectTicket(nextSelected);
    } else {
      selected.value = null;
      processRecords.value = [];
      operationLogs.value = [];
    }
  } catch (err) {
    tickets.value = [];
    page.total = 0;
    selected.value = null;
    processRecords.value = [];
    operationLogs.value = [];
    detailError.value = err.message || '加载工单失败';
  } finally {
    loading.value = false;
  }
}

async function selectTicket(ticket) {
  detailError.value = '';
  notice.value = '';
  try {
    const fullDetail = await ticketApi.fullDetail(ticket.id);
    selected.value = fullDetail?.ticket || ticket;
    processRecords.value = fullDetail?.processRecords || [];
    operationLogs.value = fullDetail?.operationLogs || [];
  } catch (err) {
    selected.value = ticket;
    processRecords.value = [];
    operationLogs.value = [];
    detailError.value = err.message || '加载详情失败';
  }
  fillActionForms();
}

function fillActionForms() {
  if (!selected.value) return;
  Object.assign(reviewForm, {
    title: selected.value.title || '',
    content: selected.value.content || '',
    aiSummary: selected.value.aiSummary || '',
    category: selected.value.category || '',
    priority: selected.value.priority || 'P3',
    suggestedAction: selected.value.suggestedAction || '',
    reviewRemark: ''
  });
  Object.assign(assignForm, { assigneeId: selected.value.assigneeId || null, skillGroupId: selected.value.skillGroupId || null, assignRemark: '' });
  Object.assign(startForm, { remark: '' });
  Object.assign(recordForm, { recordType: 'NOTE', content: '', visibleToCustomer: 0 });
  Object.assign(resolveForm, { solution: '', visibleToCustomer: 1 });
  Object.assign(closeForm, { closeReason: '' });
  autoAssignPreview.value = null;
}

async function submitReview(reviewResult) {
  if (!selected.value?.id || actionLoading.value) return;
  actionLoading.value = true;
  detailError.value = '';
  notice.value = '';
  try {
    selected.value = await ticketApi.review(selected.value.id, { ...reviewForm, reviewResult });
    notice.value = reviewResult === 'APPROVED' ? '审核通过，已进入待派单队列' : '工单已驳回';
    await loadTickets();
  } catch (err) {
    detailError.value = err.message || '审核失败';
  } finally {
    actionLoading.value = false;
  }
}

async function assignSelectedTicket() {
  if (!selected.value?.id || actionLoading.value) return;
  actionLoading.value = true;
  detailError.value = '';
  notice.value = '';
  try {
    selected.value = await ticketApi.assign(selected.value.id, { ...assignForm });
    notice.value = '派单成功';
    await loadTickets();
  } catch (err) {
    detailError.value = err.message || '派单失败';
  } finally {
    actionLoading.value = false;
  }
}

async function previewAutoAssign() {
  if (!selected.value?.id || previewLoading.value) return;
  previewLoading.value = true;
  detailError.value = '';
  try {
    autoAssignPreview.value = await ticketApi.assignmentPreview(selected.value.id);
  } catch (err) {
    autoAssignPreview.value = null;
    detailError.value = err.message || '获取自动派单预览失败';
  } finally {
    previewLoading.value = false;
  }
}

async function autoAssignSelectedTicket() {
  if (!selected.value?.id || actionLoading.value) return;
  if (!confirm('确认按当前推荐结果自动派单吗？')) return;
  actionLoading.value = true;
  detailError.value = '';
  try {
    selected.value = await ticketApi.autoAssign(selected.value.id);
    autoAssignPreview.value = null;
    notice.value = '自动派单成功';
    await loadTickets();
  } catch (err) {
    detailError.value = err.message || '自动派单失败';
  } finally {
    actionLoading.value = false;
  }
}

async function loadTicketHistories() {
  if (!selected.value?.id) {
    processRecords.value = [];
    operationLogs.value = [];
    return;
  }
  historyLoading.value = true;
  try {
    const [records, logs] = await Promise.all([
      ticketApi.records(selected.value.id, { page: 1, size: 100 }),
      ticketApi.operationLogs(selected.value.id, { page: 1, size: 100 })
    ]);
    processRecords.value = records?.records || [];
    operationLogs.value = logs?.records || [];
  } catch (err) {
    detailError.value = err.message || '加载工单时间线失败';
  } finally {
    historyLoading.value = false;
  }
}

async function refreshSelectedTicket() {
  if (!selected.value?.id) return;
  const fullDetail = await ticketApi.fullDetail(selected.value.id);
  selected.value = fullDetail?.ticket || selected.value;
  processRecords.value = fullDetail?.processRecords || [];
  operationLogs.value = fullDetail?.operationLogs || [];
  fillActionForms();
  await reloadTicketListOnly();
}

async function reloadTicketListOnly() {
  const params = { ...query };
  const data = activeQueue.value === 'pendingReview'
    ? await ticketApi.pendingReview(params)
    : activeQueue.value === 'waitingAssign'
      ? await ticketApi.waitingAssign(params)
      : await ticketApi.my(params);
  tickets.value = data?.records || [];
  page.total = data?.total || 0;
}

async function startSelectedTicket() {
  if (!selected.value?.id || actionLoading.value) return;
  actionLoading.value = true;
  detailError.value = '';
  notice.value = '';
  try {
    selected.value = await ticketApi.start(selected.value.id, { ...startForm });
    notice.value = '工单已开始处理';
    await refreshSelectedTicket();
  } catch (err) {
    detailError.value = err.message || '开始处理失败';
  } finally {
    actionLoading.value = false;
  }
}

async function addProcessRecord() {
  if (!selected.value?.id || actionLoading.value) return;
  actionLoading.value = true;
  detailError.value = '';
  notice.value = '';
  try {
    await ticketApi.addRecord(selected.value.id, { ...recordForm });
    notice.value = '处理记录已添加';
    recordForm.content = '';
    await refreshSelectedTicket();
  } catch (err) {
    detailError.value = err.message || '添加处理记录失败';
  } finally {
    actionLoading.value = false;
  }
}

async function resolveSelectedTicket() {
  if (!selected.value?.id || actionLoading.value) return;
  actionLoading.value = true;
  detailError.value = '';
  notice.value = '';
  try {
    selected.value = await ticketApi.resolve(selected.value.id, { ...resolveForm });
    notice.value = '工单已标记为解决';
    await refreshSelectedTicket();
  } catch (err) {
    detailError.value = err.message || '解决工单失败';
  } finally {
    actionLoading.value = false;
  }
}

async function closeSelectedTicket() {
  if (!selected.value?.id || actionLoading.value) return;
  actionLoading.value = true;
  detailError.value = '';
  notice.value = '';
  try {
    selected.value = await ticketApi.close(selected.value.id, { ...closeForm });
    notice.value = '工单已关闭';
    await refreshSelectedTicket();
  } catch (err) {
    detailError.value = err.message || '关闭工单失败';
  } finally {
    actionLoading.value = false;
  }
}

function searchTickets() {
  query.page = 1;
  loadTickets();
}

function resetFilters() {
  Object.assign(query, { page: 1, size: 10, keyword: '', priority: '', category: '', customerId: null, sessionId: null });
  loadTickets();
}

function changePage(nextPage) {
  query.page = nextPage;
  loadTickets();
}

function reloadCurrentQueue() {
  loadTickets();
}

async function openTicketFromNotification(ticketId) {
  if (!ticketId) return;
  loading.value = true;
  detailError.value = '';
  notice.value = '';
  try {
    const fullDetail = await ticketApi.fullDetail(ticketId);
    activeQueue.value = 'my';
    selected.value = fullDetail?.ticket || null;
    processRecords.value = fullDetail?.processRecords || [];
    operationLogs.value = fullDetail?.operationLogs || [];
    await reloadTicketListOnly();
    fillActionForms();
  } catch (err) {
    detailError.value = err.message || '打开通知关联工单失败';
  } finally {
    loading.value = false;
  }
}

function statusLabel(status) {
  return {
    PENDING_REVIEW: '待审核',
    WAITING_ASSIGN: '待派单',
    ASSIGNED: '已分配',
    PROCESSING: '处理中',
    RESOLVED: '已解决',
    CLOSED: '已关闭',
    REJECTED: '已驳回'
  }[status] || status || '-';
}

function statusClass(status) {
  return {
    off: status === 'CLOSED' || status === 'REJECTED',
    vip: status === 'ASSIGNED' || status === 'PROCESSING',
    enterprise: status === 'WAITING_ASSIGN'
  };
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function processRecordLabel(type) {
  return {
    START: '开始处理',
    NOTE: '备注',
    CONTACT_CUSTOMER: '联系客户',
    PROCESS: '处理中',
    WAITING: '等待中',
    RESOLVE: '解决工单',
    CLOSE: '关闭工单',
    OTHER: '其他'
  }[type] || type || '-';
}

function operationLabel(type) {
  return {
    CREATE: '创建工单',
    REVIEW_APPROVE: '审核通过',
    REVIEW_REJECT: '审核驳回',
    ASSIGN: '派单',
    START: '开始处理',
    ADD_RECORD: '添加记录',
    RESOLVE: '解决工单',
    CLOSE: '关闭工单'
  }[type] || type || '-';
}

onMounted(async () => {
  if (route.query.ticketId) {
    await openTicketFromNotification(route.query.ticketId);
    return;
  }
  await loadTickets();
});

watch(() => route.query.ticketId, async (ticketId) => {
  if (ticketId) await openTicketFromNotification(ticketId);
});
</script>

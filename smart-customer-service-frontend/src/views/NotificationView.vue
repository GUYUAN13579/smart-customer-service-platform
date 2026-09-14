<template>
  <section class="page-stack notification-page">
    <div class="section-header">
      <div>
        <p class="eyebrow">Work Inbox</p>
        <h2>通知中心</h2>
        <p>查看工单派发、SLA 提醒等与当前账号相关的业务通知。</p>
      </div>
      <div class="notification-header-actions">
        <span class="status-pill">{{ page.total }} 条</span>
        <button class="ghost-button" :disabled="loading" @click="loadNotifications">刷新</button>
        <button class="primary-button" :disabled="markingAll || unreadCount === 0" @click="markAllAsRead">
          {{ markingAll ? '处理中...' : '全部已读' }}
        </button>
      </div>
    </div>

    <section class="panel notification-workbench">
      <div class="notification-toolbar">
        <div>
          <p class="eyebrow">Notification Queue</p>
          <h3>我的通知</h3>
        </div>
        <AppSelect v-model="query.readStatus" :options="readStatusOptions" @change="searchNotifications" />
      </div>

      <div v-if="loading" class="empty-state">
        <strong>正在加载通知...</strong>
      </div>

      <div v-else-if="notifications.length" class="notification-list">
        <article
          v-for="item in notifications"
          :key="item.id"
          :class="['notification-item', item.readStatus === 'UNREAD' ? 'unread' : 'read']"
          @click="openNotification(item)"
        >
          <div class="notification-marker" :class="notificationTypeClass(item.type)"></div>
          <div class="notification-content">
            <div class="notification-title-row">
              <strong>{{ item.title || '系统通知' }}</strong>
              <span v-if="item.readStatus === 'UNREAD'" class="unread-label">未读</span>
            </div>
            <p>{{ item.content || '暂无通知内容' }}</p>
            <small>{{ notificationTypeLabel(item.type) }} · {{ formatTime(item.createdAt) }}</small>
          </div>
          <button
            v-if="item.readStatus === 'UNREAD'"
            class="ghost-button mini-button"
            type="button"
            :disabled="markingId === item.id"
            @click.stop="markAsRead(item)"
          >
            {{ markingId === item.id ? '处理中...' : '标为已读' }}
          </button>
          <span v-else class="notification-open-hint">查看</span>
        </article>
      </div>

      <div v-else class="empty-state">
        <strong>{{ query.readStatus === 'UNREAD' ? '没有未读通知' : '暂无通知' }}</strong>
        <span>新的工单派单和 SLA 提醒会显示在这里。</span>
      </div>

      <div class="pager">
        <span>第 {{ query.page }} 页 / 共 {{ totalPages }} 页</span>
        <button class="ghost-button" :disabled="query.page <= 1 || loading" @click="changePage(query.page - 1)">上一页</button>
        <button class="ghost-button" :disabled="query.page >= totalPages || loading" @click="changePage(query.page + 1)">下一页</button>
      </div>

      <p v-if="error" class="error-text">{{ error }}</p>
    </section>
  </section>
</template>

<script setup>
// NotificationView.vue 渲染当前登录用户的站内通知中心。
import { computed, onMounted, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import AppSelect from '../components/AppSelect.vue';
import { notificationApi } from '../api/notifications';

const router = useRouter();
const notifications = ref([]);
const loading = ref(false);
const markingAll = ref(false);
const markingId = ref(null);
const unreadCount = ref(0);
const error = ref('');
const page = reactive({ total: 0 });
const query = reactive({ page: 1, size: 10, readStatus: '' });
const readStatusOptions = [
  { label: '全部通知', value: '' },
  { label: '仅看未读', value: 'UNREAD' },
  { label: '已读通知', value: 'READ' }
];
const totalPages = computed(() => Math.max(1, Math.ceil(page.total / query.size)));

async function loadUnreadCount() {
  const data = await notificationApi.unreadCount();
  unreadCount.value = Number(data?.unreadCount || 0);
}

async function loadNotifications() {
  loading.value = true;
  error.value = '';
  try {
    const [data] = await Promise.all([notificationApi.page(query), loadUnreadCount()]);
    notifications.value = data?.records || [];
    page.total = data?.total || 0;
  } catch (err) {
    notifications.value = [];
    page.total = 0;
    error.value = err.message || '加载通知失败';
  } finally {
    loading.value = false;
  }
}

async function markAsRead(item) {
  if (item.readStatus === 'READ' || markingId.value) return;
  markingId.value = item.id;
  error.value = '';
  try {
    await notificationApi.markAsRead(item.id);
    item.readStatus = 'READ';
    item.readAt = new Date().toISOString();
    unreadCount.value = Math.max(0, unreadCount.value - 1);
    dispatchNotificationUpdated();
  } catch (err) {
    error.value = err.message || '标记通知已读失败';
  } finally {
    markingId.value = null;
  }
}

async function markAllAsRead() {
  if (markingAll.value || unreadCount.value === 0) return;
  markingAll.value = true;
  error.value = '';
  try {
    await notificationApi.markAllAsRead();
    notifications.value.forEach((item) => {
      if (item.readStatus === 'UNREAD') item.readStatus = 'READ';
    });
    unreadCount.value = 0;
    dispatchNotificationUpdated();
    if (query.readStatus === 'UNREAD') await loadNotifications();
  } catch (err) {
    error.value = err.message || '全部标记已读失败';
  } finally {
    markingAll.value = false;
  }
}

async function openNotification(item) {
  if (item.readStatus === 'UNREAD') await markAsRead(item);
  if (item.businessType === 'TICKET' && item.businessId) {
    router.push({ name: 'tickets', query: { ticketId: String(item.businessId) } });
  }
}

function searchNotifications() {
  query.page = 1;
  loadNotifications();
}

function changePage(nextPage) {
  query.page = nextPage;
  loadNotifications();
}

function notificationTypeLabel(type) {
  return {
    TICKET_ASSIGNED: '工单派单',
    SLA_RISK: 'SLA 风险提醒',
    SLA_OVERDUE: 'SLA 超时提醒'
  }[type] || type || '系统通知';
}

function notificationTypeClass(type) {
  return {
    TICKET_ASSIGNED: 'assignment',
    SLA_RISK: 'risk',
    SLA_OVERDUE: 'overdue'
  }[type] || 'system';
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function dispatchNotificationUpdated() {
  window.dispatchEvent(new Event('notification-updated'));
}

onMounted(loadNotifications);
</script>

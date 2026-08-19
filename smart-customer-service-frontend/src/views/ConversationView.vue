<template>
  <section class="conversation-page">
    <div class="section-header">
      <div>
        <p class="eyebrow">Conversation Hub</p>
        <h2>会话中心</h2>
        <p>处理客户咨询、查看聊天历史，并支持图片和文件消息发送。</p>
      </div>
      <button class="primary-button" @click="openCreate">创建会话</button>
    </div>

    <div class="conversation-shell">
      <aside class="conversation-sidebar panel">
        <div class="panel-title compact-title">
          <div>
            <p class="eyebrow">Session Queue</p>
            <h3>会话队列</h3>
          </div>
          <button class="ghost-button" @click="loadSessions">刷新</button>
        </div>

        <div class="session-filters">
          <input v-model.trim="query.keyword" placeholder="搜索会话编号" @keyup.enter="searchSessions" />
          <AppSelect v-model="query.status" :options="statusOptions" @change="searchSessions" />
          <AppSelect v-model="query.channel" :options="channelFilterOptions" @change="searchSessions" />
        </div>

        <div class="session-lookup">
          <input v-model.number="lookupId" type="number" placeholder="输入会话 ID" @keyup.enter="openById" />
          <button class="primary-button" @click="openById">打开</button>
        </div>

        <div class="session-list">
          <button
            v-for="item in sessions"
            :key="item.id"
            :class="['session-item', activeSession?.id === item.id ? 'active' : '']"
            @click="selectSession(item)"
          >
            <span class="session-title">
              {{ item.sessionNo || `会话 #${item.id}` }}
              <span class="mini-status" :class="statusClass(item.status)">{{ item.status || 'ACTIVE' }}</span>
            </span>
            <span>客户 {{ item.customerId || '-' }} · {{ item.channel || '-' }}</span>
            <small>更新 {{ formatTime(item.lastMessageAt || item.updatedAt || item.createdAt) }}</small>
          </button>
          <div v-if="!sessions.length && !loadingSessions" class="empty-state small-empty">
            <strong>暂无会话</strong>
            <span>创建会话后会出现在这里。</span>
          </div>
          <div v-if="loadingSessions" class="empty-state small-empty">
            <strong>正在加载会话...</strong>
          </div>
        </div>

        <div class="session-pager">
          <button class="ghost-button" :disabled="query.page <= 1" @click="changeSessionPage(query.page - 1)">上一页</button>
          <span>{{ query.page }} / {{ totalSessionPages }}</span>
          <button class="ghost-button" :disabled="query.page >= totalSessionPages" @click="changeSessionPage(query.page + 1)">下一页</button>
        </div>
      </aside>

      <main class="chat-panel panel">
        <div v-if="activeSession" class="chat-head">
          <div>
            <p class="eyebrow">{{ activeSession.channel || 'WEB' }}</p>
            <h3>{{ activeSession.sessionNo || `会话 #${activeSession.id}` }}</h3>
            <span>客户 {{ activeSession.customerId }} · {{ activeSession.status || 'ACTIVE' }}</span>
          </div>
          <div class="chat-head-actions">
            <span class="status-pill" :class="{ off: activeSession.status === 'CLOSED' }">{{ activeSession.status || 'ACTIVE' }}</span>
            <button v-if="canTakeOver" class="primary-button" :disabled="actionLoading" @click="takeOverActive">接管</button>
            <button v-if="canRelease" class="ghost-button" :disabled="actionLoading" @click="releaseActive">退出接管</button>
            <button v-if="activeSession.status !== 'CLOSED'" class="danger-button" :disabled="actionLoading" @click="openClose">关闭</button>
            <button class="ghost-button" @click="loadMessages">同步消息</button>
          </div>
        </div>

        <div v-if="activeSession" ref="messageScroller" class="chat-messages">
          <article
            v-for="item in messages"
            :key="item.id || `${item.createdAt}-${item.content}`"
            :class="['chat-message', messageSide(item)]"
          >
            <div class="message-meta">
              <strong>{{ senderLabel(item.senderType) }}</strong>
              <span>{{ formatTime(item.createdAt) }}</span>
            </div>

            <p v-if="messageKind(item) === 'TEXT'" class="message-text">{{ item.content }}</p>

            <a
              v-else-if="messageKind(item) === 'IMAGE'"
              class="message-image"
              :href="filePayload(item).url"
              target="_blank"
              rel="noreferrer"
            >
              <img :src="filePayload(item).url" :alt="filePayload(item).originalName || '图片消息'" />
              <span>{{ filePayload(item).originalName || '图片消息' }}</span>
            </a>

            <a
              v-else-if="messageKind(item) === 'FILE'"
              class="message-file"
              :href="filePayload(item).url"
              target="_blank"
              rel="noreferrer"
            >
              <span class="file-icon">FILE</span>
              <span>
                <strong>{{ filePayload(item).originalName || '附件' }}</strong>
                <small>{{ formatSize(filePayload(item).fileSize) }}</small>
              </span>
            </a>

            <p v-else class="message-text">{{ item.content }}</p>
          </article>

          <div v-if="!messages.length && !loadingMessages" class="empty-state">
            <strong>暂无消息</strong>
            <span>从下方输入文字，或先上传图片/文件再发送。</span>
          </div>
          <div v-if="loadingMessages" class="empty-state">
            <strong>正在加载消息...</strong>
          </div>
        </div>

        <div v-else class="chat-placeholder">
          <strong>选择或创建一个会话</strong>
          <span>会话打开后，这里会显示完整聊天记录和发送工具。</span>
        </div>

        <form v-if="canSendMessage" class="composer" @submit.prevent="sendAll">
          <div v-if="pendingFiles.length" class="pending-files">
            <div v-for="(item, index) in pendingFiles" :key="`${item.name}-${index}`" class="pending-file">
              <span>{{ item.name }}</span>
              <button class="icon-button" type="button" @click="removePending(index)">×</button>
            </div>
          </div>

          <textarea
            v-model.trim="composerText"
            rows="3"
            placeholder="输入要发送给客户的消息"
            @keydown.enter.exact.prevent="sendAll"
          ></textarea>

          <div class="composer-actions">
            <div class="upload-actions">
              <label class="upload-button">
                图片
                <input type="file" accept="image/*" multiple @change="pickImages" />
              </label>
              <label class="upload-button">
                文件
                <input type="file" multiple @change="pickFiles" />
              </label>
              <AppSelect v-model="senderType" :options="senderOptions" />
            </div>
            <button class="primary-button" :disabled="sending || (!composerText && !pendingFiles.length)">
              {{ sending ? '发送中...' : '发送' }}
            </button>
          </div>
        </form>
        <div v-else-if="activeSession" class="closed-composer">
          <strong>{{ composerLockedTitle }}</strong>
          <span>{{ composerLockedText }}</span>
        </div>
      </main>

      <aside class="conversation-detail panel">
        <p class="eyebrow">Session Detail</p>
        <h3>会话资料</h3>
        <dl class="detail-list" v-if="activeSession">
          <div><dt>会话ID</dt><dd>{{ activeSession.id }}</dd></div>
          <div><dt>客户ID</dt><dd>{{ activeSession.customerId }}</dd></div>
          <div><dt>渠道</dt><dd>{{ activeSession.channel }}</dd></div>
          <div><dt>AI启用</dt><dd>{{ activeSession.aiEnabled === 1 ? '是' : '否' }}</dd></div>
          <div><dt>客服ID</dt><dd>{{ activeSession.currentAgentId || '待接管' }}</dd></div>
          <div><dt>更新时间</dt><dd>{{ formatTime(activeSession.updatedAt || activeSession.lastMessageAt) }}</dd></div>
          <div><dt>创建时间</dt><dd>{{ formatTime(activeSession.createdAt) }}</dd></div>
        </dl>
        <div v-if="activeSession" class="detail-actions">
          <button v-if="canTakeOver" class="primary-button" :disabled="actionLoading" @click="takeOverActive">接管会话</button>
          <button v-if="canRelease" class="ghost-button" :disabled="actionLoading" @click="releaseActive">退出接管</button>
          <button v-if="activeSession.status !== 'CLOSED'" class="danger-button" :disabled="actionLoading" @click="openClose">关闭会话</button>
        </div>
        <div v-else class="empty-state small-empty">
          <strong>没有会话资料</strong>
        </div>

        <p v-if="notice" class="success-text">{{ notice }}</p>
        <p v-if="error" class="error-text">{{ error }}</p>
      </aside>
    </div>

    <div v-if="createVisible" class="modal-backdrop" @click.self="createVisible = false">
      <form class="modal" @submit.prevent="createSession">
        <div class="modal-head">
          <div>
            <p class="eyebrow">New Conversation</p>
            <h3>创建会话</h3>
          </div>
          <button class="icon-button" type="button" @click="createVisible = false">×</button>
        </div>
        <label>客户ID<input v-model.number="createForm.customerId" type="number" required placeholder="例如：1" /></label>
        <label>渠道<AppSelect v-model="createForm.channel" :options="channelOptions" /></label>
        <label class="check-row"><input v-model="createForm.aiEnabled" type="checkbox" true-value="1" false-value="0" /> 启用 AI 辅助</label>
        <p v-if="error" class="error-text">{{ error }}</p>
        <div class="modal-actions">
          <button class="ghost-button" type="button" @click="createVisible = false">取消</button>
          <button class="primary-button" :disabled="creating">{{ creating ? '创建中...' : '创建' }}</button>
        </div>
      </form>
    </div>

    <div v-if="closeVisible" class="modal-backdrop" @click.self="closeVisible = false">
      <form class="modal" @submit.prevent="closeActive">
        <div class="modal-head">
          <div>
            <p class="eyebrow">Close Session</p>
            <h3>关闭会话</h3>
          </div>
          <button class="icon-button" type="button" @click="closeVisible = false">×</button>
        </div>
        <label>关闭原因<textarea v-model.trim="closeForm.closeReason" rows="4" placeholder="可填写客户问题已解决、重复咨询、无响应等原因"></textarea></label>
        <p v-if="error" class="error-text">{{ error }}</p>
        <div class="modal-actions">
          <button class="ghost-button" type="button" @click="closeVisible = false">取消</button>
          <button class="danger-button" :disabled="actionLoading">{{ actionLoading ? '关闭中...' : '确认关闭' }}</button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from 'vue';
import AppSelect from '../components/AppSelect.vue';
import { conversationApi } from '../api/conversations';
import { fileApi } from '../api/files';
import { authStore } from '../stores/authStore';

const activeSession = ref(null);
const sessions = ref([]);
const messages = ref([]);
const pendingFiles = ref([]);
const lookupId = ref(null);
const composerText = ref('');
const senderType = ref('AGENT');
const notice = ref('');
const error = ref('');
const createVisible = ref(false);
const creating = ref(false);
const sending = ref(false);
const loadingMessages = ref(false);
const loadingSessions = ref(false);
const actionLoading = ref(false);
const closeVisible = ref(false);
const messageScroller = ref(null);
const refreshTimer = ref(null);

const createForm = reactive({ customerId: null, channel: 'WEB', aiEnabled: 1 });
const closeForm = reactive({ closeReason: '' });
const query = reactive({ page: 1, size: 10, keyword: '', status: '', channel: '' });
const sessionPage = reactive({ total: 0 });
const channelOptions = ['WEB', 'APP', 'WECHAT', 'PHONE', 'EMAIL'];
const channelFilterOptions = [
  { label: '全部渠道', value: '' },
  ...channelOptions.map((item) => ({ label: item, value: item }))
];
const statusOptions = [
  { label: '全部状态', value: '' },
  { label: '待接管 ACTIVE', value: 'ACTIVE' },
  { label: '已接管 TAKEN_OVER', value: 'TAKEN_OVER' },
  { label: '已关闭 CLOSED', value: 'CLOSED' }
];
const senderOptions = [
  { label: '客服 AGENT', value: 'AGENT' },
  { label: '客户 CUSTOMER', value: 'CUSTOMER' },
  { label: 'AI 助手', value: 'AI' }
];

const currentUser = computed(() => authStore.getUser() || {});
const totalSessionPages = computed(() => Math.max(1, Math.ceil(sessionPage.total / query.size)));
const canTakeOver = computed(() => activeSession.value && activeSession.value.status !== 'CLOSED' && activeSession.value.status !== 'TAKEN_OVER');
const canRelease = computed(() => activeSession.value
  && activeSession.value.status === 'TAKEN_OVER'
  && String(activeSession.value.currentAgentId) === String(currentUser.value.id));
const canSendMessage = computed(() => activeSession.value
  && activeSession.value.status === 'TAKEN_OVER'
  && String(activeSession.value.currentAgentId) === String(currentUser.value.id));
const composerLockedTitle = computed(() => {
  if (!activeSession.value) return '';
  if (activeSession.value.status === 'CLOSED') return '会话已关闭';
  if (activeSession.value.status !== 'TAKEN_OVER') return '请先接管会话';
  return '会话已被其他客服接管';
});
const composerLockedText = computed(() => {
  if (!activeSession.value) return '';
  if (activeSession.value.status === 'CLOSED') return '关闭后的会话不能继续发送消息。';
  if (activeSession.value.status !== 'TAKEN_OVER') return '接管成功后才能回复客户消息。';
  return '需要当前接管客服退出后，其他客服才能继续处理。';
});

function openCreate() {
  error.value = '';
  createVisible.value = true;
}

async function createSession() {
  error.value = '';
  notice.value = '';
  creating.value = true;
  try {
    const created = await conversationApi.create({
      customerId: createForm.customerId,
      channel: createForm.channel,
      aiEnabled: Number(createForm.aiEnabled)
    });
    createVisible.value = false;
    notice.value = '会话创建成功';
    await openSession(created);
    await loadSessions();
  } catch (err) {
    error.value = err.message || '创建会话失败';
  } finally {
    creating.value = false;
  }
}

async function loadSessions() {
  loadingSessions.value = true;
  error.value = '';
  try {
    const data = await conversationApi.page(query);
    sessions.value = data?.records || [];
    sessionPage.total = data?.total || 0;
    if (!activeSession.value && sessions.value.length) {
      await openSession(sessions.value[0], false);
    }
  } catch (err) {
    sessions.value = [];
    sessionPage.total = 0;
    error.value = err.message || '加载会话列表失败';
  } finally {
    loadingSessions.value = false;
  }
}

async function refreshSessionsSilently() {
  try {
    const data = await conversationApi.page(query);
    sessions.value = data?.records || [];
    sessionPage.total = data?.total || 0;
    if (activeSession.value?.id) {
      const latest = sessions.value.find((item) => item.id === activeSession.value.id);
      if (latest) {
        activeSession.value = latest;
      }
    }
  } catch {
    // 自动刷新失败时不打断当前操作。
  }
}

function searchSessions() {
  query.page = 1;
  loadSessions();
}

function changeSessionPage(page) {
  query.page = page;
  loadSessions();
}

async function openById() {
  if (!lookupId.value) return;
  error.value = '';
  notice.value = '';
  try {
    const data = await conversationApi.detail(lookupId.value);
    await openSession(data);
  } catch (err) {
    error.value = err.message || '打开会话失败';
  }
}

async function selectSession(item) {
  error.value = '';
  notice.value = '';
  try {
    const data = await conversationApi.detail(item.id);
    await openSession(data);
  } catch (err) {
    error.value = err.message || '查询会话详情失败';
  }
}

async function openSession(session, refreshList = true) {
  activeSession.value = session;
  lookupId.value = session.id;
  await loadMessages();
  if (refreshList) {
    mergeSession(session);
  }
}

async function refreshActive() {
  if (!activeSession.value?.id) return;
  await selectSession(activeSession.value);
}

async function takeOverActive() {
  if (!activeSession.value?.id) return;
  actionLoading.value = true;
  error.value = '';
  notice.value = '';
  try {
    const data = await conversationApi.takeOver(activeSession.value.id);
    activeSession.value = data;
    mergeSession(data);
    notice.value = '接管成功';
    await loadMessages();
  } catch (err) {
    error.value = err.message || '接管失败';
  } finally {
    actionLoading.value = false;
  }
}

async function releaseActive() {
  if (!activeSession.value?.id) return;
  actionLoading.value = true;
  error.value = '';
  notice.value = '';
  try {
    const data = await conversationApi.releaseTakeOver(activeSession.value.id);
    activeSession.value = data;
    mergeSession(data);
    notice.value = '已退出接管';
    await loadMessages();
  } catch (err) {
    error.value = err.message || '退出接管失败';
  } finally {
    actionLoading.value = false;
  }
}

function openClose() {
  closeForm.closeReason = '';
  error.value = '';
  closeVisible.value = true;
}

async function closeActive() {
  if (!activeSession.value?.id) return;
  actionLoading.value = true;
  error.value = '';
  notice.value = '';
  try {
    const data = await conversationApi.close(activeSession.value.id, {
      closeReason: closeForm.closeReason
    });
    closeVisible.value = false;
    activeSession.value = data;
    mergeSession(data);
    notice.value = '会话已关闭';
    await loadMessages();
  } catch (err) {
    error.value = err.message || '关闭会话失败';
  } finally {
    actionLoading.value = false;
  }
}

async function loadMessages() {
  if (!activeSession.value?.id) return;
  loadingMessages.value = true;
  error.value = '';
  try {
    messages.value = await conversationApi.messages(activeSession.value.id);
    await scrollToBottom(true);
  } catch (err) {
    messages.value = [];
    error.value = err.message || '加载消息失败';
  } finally {
    loadingMessages.value = false;
  }
}

async function refreshMessagesSilently() {
  if (!activeSession.value?.id || sending.value || actionLoading.value) return;
  const stickToBottom = isNearBottom();
  try {
    messages.value = await conversationApi.messages(activeSession.value.id);
    if (stickToBottom) {
      await scrollToBottom();
    }
  } catch {
    // 自动刷新失败时保留当前消息列表。
  }
}

function pickImages(event) {
  addPendingFiles(event.target.files, 'IMAGE');
  event.target.value = '';
}

function pickFiles(event) {
  addPendingFiles(event.target.files, 'FILE');
  event.target.value = '';
}

function addPendingFiles(fileList, messageType) {
  const files = Array.from(fileList || []);
  pendingFiles.value.push(...files.map((file) => ({ file, messageType, name: file.name })));
}

function removePending(index) {
  pendingFiles.value.splice(index, 1);
}

async function sendAll() {
  if (!activeSession.value?.id) return;
  if (!canSendMessage.value) {
    error.value = activeSession.value.status === 'CLOSED' ? '会话已关闭，不能发送消息' : '请先接管会话后再发送消息';
    return;
  }
  error.value = '';
  notice.value = '';
  sending.value = true;
  try {
    for (const item of pendingFiles.value) {
      const uploaded = item.messageType === 'IMAGE'
        ? await fileApi.uploadImage(item.file, currentUser.value.id)
        : await fileApi.uploadFile(item.file, currentUser.value.id);
      await sendConversationMessage(item.messageType, JSON.stringify(uploaded));
    }
    if (composerText.value) {
      await sendConversationMessage('TEXT', composerText.value);
    }
    composerText.value = '';
    pendingFiles.value = [];
    notice.value = '消息发送成功';
    await refreshActive();
    await loadMessages();
  } catch (err) {
    error.value = err.message || '发送失败';
  } finally {
    sending.value = false;
  }
}

function sendConversationMessage(messageType, content) {
  return conversationApi.sendMessage(activeSession.value.id, {
    senderType: senderType.value,
    senderId: senderId(),
    messageType,
    content
  });
}

function senderId() {
  if (senderType.value === 'AGENT') return currentUser.value.id || null;
  if (senderType.value === 'CUSTOMER') return activeSession.value?.customerId || null;
  return null;
}

function messageSide(item) {
  if (item.senderType === 'CUSTOMER') return 'incoming';
  if (item.senderType === 'SYSTEM') return 'system';
  return 'outgoing';
}

function senderLabel(type) {
  return {
    CUSTOMER: '客户',
    AGENT: '客服',
    AI: 'AI 助手',
    SYSTEM: '系统'
  }[type] || type || '未知';
}

function messageKind(item) {
  return item.messageType || 'TEXT';
}

function filePayload(item) {
  if (!item?.content) return {};
  let payload;
  try {
    payload = JSON.parse(item.content);
  } catch {
    payload = { url: item.content, originalName: item.content };
  }
  const fileId = payload.id || payload.fileId;
  if (fileId) {
    payload.url = `/api/v1/customer-test/conversations/files/${fileId}/content`;
  }
  return payload;
}

function mergeSession(session) {
  const index = sessions.value.findIndex((item) => item.id === session.id);
  if (index >= 0) {
    sessions.value.splice(index, 1, session);
  } else {
    sessions.value.unshift(session);
  }
}

function statusClass(status) {
  return {
    closed: status === 'CLOSED',
    taken: status === 'TAKEN_OVER'
  };
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

function formatSize(value) {
  if (!value) return '';
  if (value < 1024) return `${value} B`;
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1024 / 1024).toFixed(1)} MB`;
}

function isNearBottom() {
  const scroller = messageScroller.value;
  if (!scroller) return true;
  return scroller.scrollHeight - scroller.scrollTop - scroller.clientHeight < 80;
}

async function scrollToBottom(force = false) {
  await nextTick();
  if (messageScroller.value && (force || isNearBottom())) {
    messageScroller.value.scrollTop = messageScroller.value.scrollHeight;
  }
}

function startAutoRefresh() {
  stopAutoRefresh();
  refreshTimer.value = window.setInterval(() => {
    refreshSessionsSilently();
    refreshMessagesSilently();
  }, 3000);
}

function stopAutoRefresh() {
  if (refreshTimer.value) {
    window.clearInterval(refreshTimer.value);
    refreshTimer.value = null;
  }
}

onMounted(() => {
  loadSessions();
  startAutoRefresh();
});

onUnmounted(stopAutoRefresh);
</script>

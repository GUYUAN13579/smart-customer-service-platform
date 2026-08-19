<template>
  <section class="customer-chat-page">
    <header class="customer-chat-header">
      <div>
        <p class="eyebrow">Customer Portal</p>
        <h1>用户端会话测试</h1>
        <p>模拟客户发起咨询、发送文字、上传图片或附件，用来测试后台会话中心。</p>
      </div>
      <RouterLink class="ghost-button" to="/login">返回后台登录</RouterLink>
    </header>

    <div class="customer-chat-shell">
      <aside class="panel customer-chat-side">
        <div>
          <p class="eyebrow">Start Session</p>
          <h3>发起咨询</h3>
        </div>

        <label>客户ID<input v-model.number="createForm.customerId" type="number" placeholder="必须是数据库已有客户ID" /></label>
        <label>渠道<AppSelect v-model="createForm.channel" :options="channelOptions" /></label>
        <label class="check-row"><input v-model="createForm.aiEnabled" type="checkbox" true-value="1" false-value="0" /> 启用 AI 辅助</label>
        <button class="primary-button" :disabled="creating || !createForm.customerId" @click="createSession">
          {{ creating ? '创建中...' : '创建会话' }}
        </button>

        <div class="session-lookup">
          <input v-model.number="lookupId" type="number" placeholder="已有会话ID" @keyup.enter="openSessionById" />
          <button class="ghost-button" @click="openSessionById">打开</button>
        </div>

        <dl v-if="session" class="detail-list compact-detail">
          <div><dt>会话ID</dt><dd>{{ session.id }}</dd></div>
          <div><dt>会话编号</dt><dd>{{ session.sessionNo }}</dd></div>
          <div><dt>状态</dt><dd>{{ session.status }}</dd></div>
          <div><dt>客服ID</dt><dd>{{ session.currentAgentId || '等待客服接管' }}</dd></div>
        </dl>
      </aside>

      <main class="panel customer-chat-main">
        <div class="chat-head" v-if="session">
          <div>
            <p class="eyebrow">{{ session.channel }}</p>
            <h3>{{ session.sessionNo || `会话 #${session.id}` }}</h3>
            <span>客户 {{ session.customerId }} · {{ session.status }}</span>
          </div>
          <button class="ghost-button" @click="loadMessages">刷新消息</button>
        </div>

        <div v-if="session" ref="messageScroller" class="chat-messages customer-chat-messages">
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
            <a v-else-if="messageKind(item) === 'IMAGE'" class="message-image" :href="filePayload(item).url" target="_blank" rel="noreferrer">
              <img :src="filePayload(item).url" :alt="filePayload(item).originalName || '图片消息'" />
              <span>{{ filePayload(item).originalName || '图片消息' }}</span>
            </a>
            <a v-else-if="messageKind(item) === 'FILE'" class="message-file" :href="filePayload(item).url" target="_blank" rel="noreferrer">
              <span class="file-icon">FILE</span>
              <span>
                <strong>{{ filePayload(item).originalName || '附件' }}</strong>
                <small>{{ formatSize(filePayload(item).fileSize) }}</small>
              </span>
            </a>
            <p v-else class="message-text">{{ item.content }}</p>
          </article>

          <div v-if="!messages.length" class="empty-state">
            <strong>暂无消息</strong>
            <span>发送第一条消息后，后台会话中心就能看到它。</span>
          </div>
        </div>

        <div v-else class="chat-placeholder">
          <strong>先创建或打开一个会话</strong>
          <span>用户端测试页会通过公开测试接口模拟客户消息。</span>
        </div>

        <form v-if="session && session.status !== 'CLOSED'" class="composer" @submit.prevent="sendAll">
          <div v-if="pendingFiles.length" class="pending-files">
            <div v-for="(item, index) in pendingFiles" :key="`${item.name}-${index}`" class="pending-file">
              <span>{{ item.name }}</span>
              <button class="icon-button" type="button" @click="removePending(index)">×</button>
            </div>
          </div>

          <textarea
            v-model.trim="composerText"
            rows="3"
            placeholder="请输入你的问题，例如：我的订单无法退款"
            @keydown.enter.exact.prevent="sendAll"
          ></textarea>
          <div class="composer-actions">
            <div class="upload-actions">
              <label class="upload-button">图片<input type="file" accept="image/*" multiple @change="pickImages" /></label>
              <label class="upload-button">文件<input type="file" multiple @change="pickFiles" /></label>
            </div>
            <button class="primary-button" :disabled="sending || (!composerText && !pendingFiles.length)">
              {{ sending ? '发送中...' : '发送' }}
            </button>
          </div>
        </form>

        <div v-else-if="session" class="closed-composer">
          <strong>会话已关闭</strong>
          <span>关闭后的会话不能继续发送消息。</span>
        </div>
      </main>
    </div>

    <p v-if="notice" class="success-text floating-message">{{ notice }}</p>
    <p v-if="error" class="error-text floating-message">{{ error }}</p>
  </section>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, reactive, ref } from 'vue';
import AppSelect from '../components/AppSelect.vue';
import { customerTestApi } from '../api/customerTest';

const session = ref(null);
const messages = ref([]);
const pendingFiles = ref([]);
const lookupId = ref(null);
const composerText = ref('');
const creating = ref(false);
const sending = ref(false);
const notice = ref('');
const error = ref('');
const messageScroller = ref(null);
const refreshTimer = ref(null);

const createForm = reactive({ customerId: null, channel: 'WEB', aiEnabled: 1 });
const channelOptions = ['WEB', 'APP', 'WECHAT', 'PHONE', 'EMAIL'];

async function createSession() {
  creating.value = true;
  clearTip();
  try {
    session.value = await customerTestApi.createSession({
      customerId: createForm.customerId,
      channel: createForm.channel,
      aiEnabled: Number(createForm.aiEnabled)
    });
    lookupId.value = session.value.id;
    notice.value = '会话创建成功';
    await loadMessages();
  } catch (err) {
    error.value = err.message || '创建会话失败';
  } finally {
    creating.value = false;
  }
}

async function openSessionById() {
  if (!lookupId.value) return;
  clearTip();
  try {
    session.value = await customerTestApi.detail(lookupId.value);
    await loadMessages();
  } catch (err) {
    error.value = err.message || '打开会话失败';
  }
}

async function loadMessages() {
  if (!session.value?.id) return;
  messages.value = await customerTestApi.messages(session.value.id);
  await scrollToBottom(true);
}

async function refreshSilently() {
  if (!session.value?.id || sending.value) return;
  const stickToBottom = isNearBottom();
  try {
    session.value = await customerTestApi.detail(session.value.id);
    messages.value = await customerTestApi.messages(session.value.id);
    if (stickToBottom) {
      await scrollToBottom();
    }
  } catch {
    // 自动刷新失败不打断当前聊天。
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
  pendingFiles.value.push(...Array.from(fileList || []).map((file) => ({ file, messageType, name: file.name })));
}

function removePending(index) {
  pendingFiles.value.splice(index, 1);
}

async function sendAll() {
  if (!session.value?.id) return;
  sending.value = true;
  clearTip();
  try {
    for (const item of pendingFiles.value) {
      const uploaded = item.messageType === 'IMAGE'
        ? await customerTestApi.uploadImage(item.file, session.value.customerId)
        : await customerTestApi.uploadFile(item.file, session.value.customerId);
      await sendMessage(item.messageType, JSON.stringify(uploaded));
    }
    if (composerText.value) {
      await sendMessage('TEXT', composerText.value);
    }
    composerText.value = '';
    pendingFiles.value = [];
    session.value = await customerTestApi.detail(session.value.id);
    await loadMessages();
    notice.value = '发送成功';
  } catch (err) {
    error.value = err.message || '发送失败';
  } finally {
    sending.value = false;
  }
}

function sendMessage(messageType, content) {
  return customerTestApi.sendMessage(session.value.id, {
    senderType: 'CUSTOMER',
    senderId: session.value.customerId,
    messageType,
    content
  });
}

function messageSide(item) {
  if (item.senderType === 'CUSTOMER') return 'outgoing';
  if (item.senderType === 'SYSTEM') return 'system';
  return 'incoming';
}

function senderLabel(type) {
  return {
    CUSTOMER: '我',
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

function clearTip() {
  notice.value = '';
  error.value = '';
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
  refreshTimer.value = window.setInterval(refreshSilently, 3000);
}

function stopAutoRefresh() {
  if (refreshTimer.value) {
    window.clearInterval(refreshTimer.value);
    refreshTimer.value = null;
  }
}

onMounted(startAutoRefresh);
onUnmounted(stopAutoRefresh);
</script>

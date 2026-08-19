<template>
  <section class="page-stack">
    <div class="section-header">
      <div>
        <p class="eyebrow">AI Assistant</p>
        <h2>AI 工作台</h2>
        <p>用于客服坐席快速生成回复建议和工单草稿，后续可以接入知识库检索与工具调用。</p>
      </div>
    </div>

    <div class="two-column">
      <div class="panel">
        <div class="panel-title">
          <div>
            <h2>会话问答</h2>
            <p>调用 `POST /api/v1/ai/chat`。</p>
          </div>
        </div>
        <div class="form-grid compact-grid">
          <label>会话ID<input v-model.number="chatForm.sessionId" type="number" /></label>
          <label class="check-row"><input v-model="chatForm.enableToolCalling" type="checkbox" /> 启用工具调用</label>
        </div>
        <label>客户问题<textarea v-model.trim="chatForm.message" rows="7" placeholder="客户反馈无法登录，验证码一直收不到"></textarea></label>
        <button class="primary-button" :disabled="chatLoading" @click="sendChat">{{ chatLoading ? '生成中...' : '生成回复建议' }}</button>
        <div class="ai-result">
          <strong>AI 回复</strong>
          <p>{{ chatResult || '生成结果会显示在这里。' }}</p>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">
          <div>
            <h2>工单草稿</h2>
            <p>调用 `POST /api/v1/ai/ticket-draft`。</p>
          </div>
        </div>
        <div class="form-grid compact-grid">
          <label>会话ID<input v-model.number="draftForm.sessionId" type="number" /></label>
          <label>客户ID<input v-model.number="draftForm.customerId" type="number" /></label>
        </div>
        <label>问题内容<textarea v-model.trim="draftForm.content" rows="7" placeholder="请粘贴客户完整问题或会话摘要"></textarea></label>
        <button class="primary-button" :disabled="draftLoading" @click="createDraft">{{ draftLoading ? '生成中...' : '生成工单草稿' }}</button>
        <div class="draft-card">
          <div><span>标题</span><strong>{{ draft?.title || '-' }}</strong></div>
          <div><span>分类</span><strong>{{ draft?.category || '-' }}</strong></div>
          <div><span>优先级</span><strong>{{ draft?.priority || '-' }}</strong></div>
          <p>{{ draft?.content || draftText || '草稿内容会显示在这里。' }}</p>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { aiApi } from '../api/ai';

const chatLoading = ref(false);
const draftLoading = ref(false);
const chatResult = ref('');
const draft = ref(null);
const draftText = ref('');
const chatForm = reactive({ sessionId: null, message: '', enableToolCalling: true });
const draftForm = reactive({ sessionId: null, customerId: null, content: '' });

async function sendChat() {
  chatLoading.value = true;
  chatResult.value = '';
  try {
    const data = await aiApi.chat(chatForm);
    chatResult.value = typeof data === 'string' ? data : data.answer || data.content || JSON.stringify(data, null, 2);
  } catch (err) {
    chatResult.value = err.message || 'AI 问答接口暂不可用';
  } finally {
    chatLoading.value = false;
  }
}

async function createDraft() {
  draftLoading.value = true;
  draft.value = null;
  draftText.value = '';
  try {
    const data = await aiApi.ticketDraft(draftForm);
    draft.value = data && typeof data === 'object' ? data : null;
    draftText.value = typeof data === 'string' ? data : '';
  } catch (err) {
    draftText.value = err.message || 'AI 草稿接口暂不可用';
  } finally {
    draftLoading.value = false;
  }
}
</script>

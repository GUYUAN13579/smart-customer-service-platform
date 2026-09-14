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
          <label>客户问题<textarea v-model.trim="chatForm.question" rows="7" placeholder="客户反馈无法登录，验证码一直收不到"></textarea></label>
        <button class="primary-button" :disabled="chatLoading" @click="sendChat">{{ chatLoading ? '生成中...' : '生成回复建议' }}</button>
        <div class="ai-result">
          <strong>AI 回复</strong>
          <p>{{ chatResult?.answer || '生成结果会显示在这里。' }}</p>
          <div v-if="chatResult?.knowledgeReferences?.length" class="ai-knowledge-references">
            <span>知识依据</span>
            <button v-for="reference in chatResult.knowledgeReferences" :key="reference.chunkId" type="button" @click="showReference(reference)">
              {{ reference.sourceTitle || `知识片段 #${reference.chunkId}` }}
            </button>
          </div>
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
          <label>问题内容<input v-model.trim="draftForm.content" placeholder="可选：保留客户原始表达" /></label>
        </div>
        <label>补充要求<textarea v-model.trim="draftForm.extraRequirement" rows="7" placeholder="例如：请重点梳理退款诉求与需要确认的信息"></textarea></label>
        <button class="primary-button" :disabled="draftLoading" @click="createDraft">{{ draftLoading ? '生成中...' : '生成工单草稿' }}</button>
        <div class="draft-card">
          <div><span>标题</span><strong>{{ draft?.title || '-' }}</strong></div>
          <div><span>分类</span><strong>{{ draft?.category || '-' }}</strong></div>
          <div><span>优先级</span><strong>{{ draft?.priority || '-' }}</strong></div>
          <p>{{ draft?.aiSummary || draftText || '草稿内容会显示在这里。' }}</p>
          <div v-if="draft?.knowledgeReferences?.length" class="ai-knowledge-references">
            <span>知识依据</span>
            <button v-for="reference in draft.knowledgeReferences" :key="reference.chunkId" type="button" @click="showReference(reference)">
              {{ reference.sourceTitle || `知识片段 #${reference.chunkId}` }}
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="referenceText" class="panel">
      <div class="panel-title"><div><p class="eyebrow">Knowledge Reference</p><h2>知识原文</h2></div><button class="ghost-button" @click="referenceText = ''">关闭</button></div>
      <p class="reference-content">{{ referenceText }}</p>
    </div>
  </section>
</template>

<script setup>
// AiWorkbenchView.vue 渲染对应的业务工作台页面。
import { reactive, ref } from 'vue';
import { aiApi } from '../api/ai';

const chatLoading = ref(false);
const draftLoading = ref(false);
const chatResult = ref(null);
const draft = ref(null);
const draftText = ref('');
const referenceText = ref('');
const chatForm = reactive({ sessionId: null, question: '' });
const draftForm = reactive({ sessionId: null, content: '', extraRequirement: '' });

async function sendChat() {
  chatLoading.value = true;
  chatResult.value = null;
  try {
    const data = await aiApi.chat(chatForm);
    chatResult.value = typeof data === 'string' ? { answer: data } : data;
  } catch (err) {
    chatResult.value = { answer: err.message || 'AI 问答接口暂不可用' };
  } finally {
    chatLoading.value = false;
  }
}

async function createDraft() {
  draftLoading.value = true;
  draft.value = null;
  draftText.value = '';
  try {
    if (!draftForm.sessionId) throw new Error('请先填写会话ID');
    const data = await aiApi.ticketDraft(draftForm.sessionId, {
      originalContent: draftForm.content || undefined,
      extraRequirement: draftForm.extraRequirement || undefined
    });
    draft.value = data && typeof data === 'object' ? data : null;
    draftText.value = typeof data === 'string' ? data : '';
  } catch (err) {
    draftText.value = err.message || 'AI 草稿接口暂不可用';
  } finally {
    draftLoading.value = false;
  }
}

function showReference(reference) {
  referenceText.value = `${reference.sourceTitle || '未命名知识'}\n\n${reference.content || ''}`;
}
</script>

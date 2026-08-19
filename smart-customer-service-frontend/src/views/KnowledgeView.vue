<template>
  <section class="page-stack">
    <div class="section-header">
      <div>
        <p class="eyebrow">Knowledge Base</p>
        <h2>知识库</h2>
        <p>沉淀常见问题、处理手册和 AI 引用资料，支撑坐席检索和智能草稿生成。</p>
      </div>
      <button class="primary-button" @click="openArticle">新增文章</button>
    </div>

    <div class="two-column">
      <div class="panel">
        <div class="panel-title">
          <div>
            <h2>语义检索</h2>
            <p>调用 `POST /api/v1/knowledge/search`。</p>
          </div>
          <button class="ghost-button" @click="search">检索</button>
        </div>
        <div class="form-grid compact-grid">
          <label>关键词<input v-model.trim="searchForm.keyword" placeholder="验证码收不到" /></label>
          <label>分类ID<input v-model.number="searchForm.categoryId" type="number" /></label>
          <label>TopK<input v-model.number="searchForm.topK" type="number" min="1" max="20" /></label>
        </div>
        <div class="knowledge-list">
          <article v-for="item in results" :key="item.articleId || item.id">
            <strong>{{ item.title }}</strong>
            <p>{{ item.snippet || item.summary || item.content || '暂无摘要' }}</p>
            <span>score {{ item.score ?? '-' }}</span>
          </article>
          <div v-if="!results.length" class="empty-state">
            <strong>{{ message || '暂无检索结果' }}</strong>
            <span>知识检索接口完成后，这里会展示匹配片段和分数。</span>
          </div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-title">
          <div>
            <h2>文章管理</h2>
            <p>调用文章分页、发布和下线接口。</p>
          </div>
          <button class="ghost-button" @click="loadArticles">刷新</button>
        </div>
        <div class="article-list">
          <article v-for="article in articles" :key="article.id">
            <div>
              <strong>{{ article.title }}</strong>
              <span>{{ article.status || 'DRAFT' }} · 分类 {{ article.categoryId || '-' }}</span>
            </div>
            <div class="actions">
              <button @click="publish(article.id)">发布</button>
              <button @click="offline(article.id)">下线</button>
            </div>
          </article>
          <div v-if="!articles.length" class="empty-state">
            <strong>暂无文章</strong>
            <span>文章接口完成后，这里会显示知识库文章列表。</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="dialogVisible" class="modal-backdrop" @click.self="dialogVisible = false">
      <form class="modal large-modal" @submit.prevent="createArticle">
        <div class="modal-head">
          <div>
            <p class="eyebrow">New Article</p>
            <h3>新增知识文章</h3>
          </div>
          <button class="icon-button" type="button" @click="dialogVisible = false">×</button>
        </div>
        <div class="form-grid compact-grid">
          <label>分类ID<input v-model.number="articleForm.categoryId" type="number" /></label>
          <label>标题<input v-model.trim="articleForm.title" required /></label>
        </div>
        <label>内容<textarea v-model.trim="articleForm.content" required rows="8"></textarea></label>
        <p v-if="error" class="error-text">{{ error }}</p>
        <div class="modal-actions">
          <button class="ghost-button" type="button" @click="dialogVisible = false">取消</button>
          <button class="primary-button">保存文章</button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { knowledgeApi } from '../api/knowledge';

const results = ref([]);
const articles = ref([]);
const message = ref('');
const error = ref('');
const dialogVisible = ref(false);
const searchForm = reactive({ keyword: '', categoryId: null, topK: 5 });
const articleQuery = reactive({ page: 1, size: 20 });
const articleForm = reactive({ categoryId: null, title: '', content: '' });

function openArticle() {
  Object.assign(articleForm, { categoryId: null, title: '', content: '' });
  error.value = '';
  dialogVisible.value = true;
}

async function search() {
  message.value = '';
  try {
    results.value = await knowledgeApi.search(searchForm);
    if (!results.value?.length) message.value = '暂无匹配知识';
  } catch (err) {
    results.value = [];
    message.value = err.message || '检索接口暂不可用';
  }
}

async function loadArticles() {
  try {
    const data = await knowledgeApi.pageArticles(articleQuery);
    articles.value = data?.records || [];
  } catch {
    articles.value = [];
  }
}

async function createArticle() {
  error.value = '';
  try {
    await knowledgeApi.createArticle(articleForm);
    dialogVisible.value = false;
    await loadArticles();
  } catch (err) {
    error.value = err.message || '保存失败';
  }
}

async function publish(id) {
  await knowledgeApi.publishArticle(id);
  await loadArticles();
}

async function offline(id) {
  await knowledgeApi.offlineArticle(id);
  await loadArticles();
}

onMounted(loadArticles);
</script>

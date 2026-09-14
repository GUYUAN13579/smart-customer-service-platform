<template>
  <section class="page-stack">
    <div class="section-header">
      <div>
        <p class="eyebrow">Knowledge Base</p>
        <h2>知识库管理</h2>
        <p>完成文章或纯文本文件的解析、切片、向量索引与语义检索，为 AI 回答提供可追溯的业务依据。</p>
      </div>
      <button class="primary-button" @click="openCreate">新增文章</button>
    </div>

    <div class="panel">
      <div class="panel-title"><div><p class="eyebrow">Articles</p><h2>知识文章</h2></div></div>
      <div class="filter-bar knowledge-filter-bar">
        <label>关键词<input v-model.trim="query.keyword" placeholder="标题或摘要" @keyup.enter="loadArticles" /></label>
        <label>分类<AppSelect v-model="query.category" :options="categoryFilterOptions" @change="loadArticles" /></label>
        <label>状态<AppSelect v-model="query.status" :options="articleStatusOptions" @change="loadArticles" /></label>
        <div class="filter-actions"><button class="ghost-button" @click="resetQuery">重置</button><button class="primary-button" @click="loadArticles">查询</button></div>
      </div>
      <div class="table-wrap">
        <table class="data-table knowledge-table">
          <thead><tr><th>文章</th><th>分类</th><th>状态</th><th>版本</th><th>发布时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="article in articles" :key="article.id">
              <td class="article-cell"><strong>{{ article.title }}</strong><span>{{ article.summary || '暂无摘要' }}</span></td>
              <td><span class="category-code">{{ article.category }}</span></td>
              <td><span class="status-pill" :class="statusClass(article.status)">{{ statusText(article.status) }}</span></td>
              <td>v{{ article.version || 1 }}</td>
              <td>{{ formatTime(article.publishedAt) }}</td>
              <td class="actions article-actions">
                <button @click="editArticle(article.id)">编辑</button>
                <button v-if="article.status !== 'PUBLISHED'" @click="publishArticle(article)">发布</button>
                <button v-else @click="offlineArticle(article)">下线</button>
                <button v-if="article.status === 'PUBLISHED'" @click="loadChunks('ARTICLE', article.id, article.title)">切片</button>
                <button v-if="article.status === 'PUBLISHED'" @click="rebuildArticle(article)">重建切片</button>
                <button v-if="article.status === 'PUBLISHED'" class="primary-mini" @click="createArticleIndex(article)">创建索引</button>
                <button class="danger-button" @click="deleteArticle(article)">删除</button>
              </td>
            </tr>
            <tr v-if="loading"><td colspan="6" class="empty-cell">正在加载知识文章...</td></tr>
            <tr v-else-if="!articles.length"><td colspan="6" class="empty-cell">暂无知识文章</td></tr>
          </tbody>
        </table>
      </div>
      <div class="pager"><span>共 {{ page.total }} 条</span><button class="ghost-button" :disabled="query.page <= 1" @click="changePage(query.page - 1)">上一页</button><span>第 {{ query.page }} 页</span><button class="ghost-button" :disabled="query.page * query.size >= page.total" @click="changePage(query.page + 1)">下一页</button></div>
    </div>

    <div class="panel">
      <div class="panel-title">
        <div><p class="eyebrow">Documents</p><h2>纯文本文件</h2><p>仅支持 TXT、MD、CSV、JSON、XML、LOG 文件。</p></div>
        <div class="upload-actions">
          <input ref="documentInput" class="visually-hidden" type="file" accept=".txt,.md,.csv,.json,.xml,.log,text/plain,text/markdown,text/csv,application/json,application/xml" @change="selectDocumentFile" />
          <button class="ghost-button" @click="documentInput?.click()">{{ pendingDocument?.name || '选择文件' }}</button>
          <button class="primary-button" :disabled="!pendingDocument || documentImporting" @click="importDocument">{{ documentImporting ? '导入中...' : '导入知识文件' }}</button>
        </div>
      </div>
      <p v-if="documentTip" class="success-text">{{ documentTip }}</p>
      <p v-if="documentError" class="error-text">{{ documentError }}</p>
      <div class="table-wrap">
        <table class="data-table document-table">
          <thead><tr><th>文件</th><th>状态</th><th>解析器</th><th>更新时间</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="document in documents" :key="document.id">
              <td class="article-cell"><strong>{{ document.documentName }}</strong><span>{{ document.contentType || '未知类型' }}</span></td>
              <td><span class="status-pill" :class="documentStatusClass(document.status)">{{ document.status }}</span></td>
              <td>{{ document.parserType || '-' }}</td><td>{{ formatTime(document.updatedAt) }}</td>
              <td class="actions article-actions">
                <button v-if="document.status === 'UPLOADED'" @click="parseDocument(document)">解析</button>
                <button v-if="document.status === 'FAILED'" @click="retryParseDocument(document)">重试解析</button>
                <button v-if="document.status === 'PARSED'" @click="loadChunks('DOCUMENT', document.id, document.documentName)">切片</button>
                <button v-if="document.status === 'PARSED'" @click="rebuildDocument(document)">重建切片</button>
                <button v-if="document.status === 'PARSED'" class="primary-mini" @click="createDocumentIndex(document)">创建索引</button>
                <button v-if="document.status !== 'OFFLINE'" @click="offlineDocument(document)">下线</button>
                <button class="danger-button" @click="deleteDocument(document)">删除</button>
              </td>
            </tr>
            <tr v-if="documentsLoading"><td colspan="5" class="empty-cell">正在加载导入文件...</td></tr>
            <tr v-else-if="!documents.length"><td colspan="5" class="empty-cell">暂无导入文件</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="panel">
      <div class="panel-title"><div><p class="eyebrow">Indexing</p><h2>索引任务</h2><p>创建任务后系统会自动索引；失败任务将自动重试，管理员也可手动重新触发。</p></div><button class="ghost-button" @click="loadIndexTasks">刷新任务</button></div>
      <div class="table-wrap">
        <table class="data-table index-task-table">
          <thead><tr><th>任务</th><th>来源</th><th>状态</th><th>重试</th><th>失败原因</th><th>操作</th></tr></thead>
          <tbody>
            <tr v-for="task in indexTasks" :key="task.id">
              <td><strong>#{{ task.id }}</strong><small>{{ task.taskType }}</small></td><td>{{ task.sourceType }} / {{ task.sourceId }}</td>
              <td><span class="status-pill" :class="taskStatusClass(task.status)">{{ task.status }}</span></td>
              <td>{{ task.retryCount || 0 }} / {{ task.maxRetryCount || 3 }}</td><td>{{ task.errorMessage || '-' }}</td>
              <td class="actions"><button v-if="task.status === 'PENDING'" class="primary-mini" @click="executeIndexTask(task)">执行</button><button v-if="task.status === 'FAILED'" @click="retryIndexTask(task)">重试</button></td>
            </tr>
            <tr v-if="tasksLoading"><td colspan="6" class="empty-cell">正在加载索引任务...</td></tr>
            <tr v-else-if="!indexTasks.length"><td colspan="6" class="empty-cell">暂无索引任务</td></tr>
          </tbody>
        </table>
      </div>
    </div>

    <div class="two-column knowledge-operations-grid">
      <div class="panel">
        <div class="panel-title"><div><p class="eyebrow">Search Debug</p><h2>语义检索测试</h2><p>验证 qwen 向量化与 Elasticsearch 命中结果。</p></div></div>
        <label>检索问题<textarea v-model.trim="searchForm.query" rows="5" placeholder="例如：收不到验证码该怎么处理？"></textarea></label>
        <div class="form-grid compact-grid"><label>结果数量<input v-model.number="searchForm.topK" type="number" min="1" max="20" /></label><label>来源类型<AppSelect v-model="searchForm.sourceType" :options="sourceTypeOptions" /></label></div>
        <label>文章分类<AppSelect v-model="searchForm.category" :options="categoryFilterOptions" /></label>
        <button class="primary-button" :disabled="searchLoading || !searchForm.query" @click="runSearch">{{ searchLoading ? '检索中...' : '开始检索' }}</button>
        <p v-if="searchError" class="error-text">{{ searchError }}</p>
        <div v-if="searchResults.length" class="knowledge-search-results">
          <article v-for="result in searchResults" :key="result.chunkId"><div><strong>{{ result.sourceTitle || '未命名知识' }}</strong><span>相似度 {{ Number(result.score || 0).toFixed(4) }} · {{ result.sourceType }} #{{ result.sourceId }}</span></div><p>{{ result.content }}</p></article>
        </div>
        <div v-else-if="searchCompleted" class="empty-state compact"><strong>没有命中结果</strong><span>请先完成切片与索引任务，或换一个更具体的问题。</span></div>
      </div>

      <div class="panel">
        <div class="panel-title"><div><p class="eyebrow">Chunks</p><h2>知识切片</h2><p>{{ selectedSource ? `${selectedSource.label} 的切片内容` : '从文章或文件列表选择“切片”查看。' }}</p></div><button v-if="selectedSource" class="ghost-button" @click="reloadSelectedChunks">刷新</button></div>
        <div v-if="selectedSource" class="knowledge-chunk-list">
          <article v-for="chunk in chunks" :key="chunk.id"><div><strong>片段 {{ chunk.chunkNo + 1 }}</strong><span class="status-pill" :class="taskStatusClass(chunk.indexStatus)">{{ chunk.indexStatus }}</span></div><p>{{ chunk.content }}</p><small v-if="chunk.indexError" class="error-text">{{ chunk.indexError }}</small></article>
          <div v-if="chunksLoading" class="empty-state compact">正在加载切片...</div><div v-else-if="!chunks.length" class="empty-state compact">暂无切片。请先执行重建切片。</div>
        </div>
        <div v-else class="empty-state compact"><strong>尚未选择来源</strong><span>已发布文章或已解析文件都可以生成切片。</span></div>
      </div>
    </div>

    <div v-if="dialogVisible" class="modal-backdrop" @click.self="dialogVisible = false">
      <form class="modal large-modal knowledge-article-modal" @submit.prevent="saveArticle">
        <div class="modal-head"><div><p class="eyebrow">{{ form.id ? 'Edit Article' : 'New Article' }}</p><h3>{{ form.id ? '编辑知识文章' : '新增知识文章' }}</h3></div><button class="icon-button" type="button" @click="dialogVisible = false">×</button></div>
        <div class="form-grid compact-grid"><label>文章标题<input v-model.trim="form.title" required maxlength="255" placeholder="例如：收不到验证码的排查步骤" /></label><label>分类<AppSelect v-model="form.category" :options="categoryOptions" /></label></div>
        <div class="form-grid compact-grid"><label>文章摘要<input v-model.trim="form.summary" maxlength="1000" placeholder="一句话概括文章用途" /></label><label>标签<input v-model.trim="form.tagsText" placeholder="验证码, 登录, 短信" /></label></div>
        <label>文章正文<textarea v-model.trim="form.content" required rows="14" placeholder="支持 Markdown 或纯文本内容"></textarea></label>
        <p v-if="error" class="error-text">{{ error }}</p><div class="modal-actions"><button class="ghost-button" type="button" @click="dialogVisible = false">取消</button><button class="primary-button" :disabled="saving">{{ saving ? '保存中...' : '保存草稿' }}</button></div>
      </form>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppSelect from '../components/AppSelect.vue';
import { authStore } from '../stores/authStore';
import { fileApi } from '../api/files';
import { knowledgeApi } from '../api/knowledge';

const articles = ref([]); const documents = ref([]); const indexTasks = ref([]); const chunks = ref([]); const searchResults = ref([]);
const loading = ref(false); const saving = ref(false); const documentsLoading = ref(false); const tasksLoading = ref(false); const chunksLoading = ref(false); const searchLoading = ref(false); const documentImporting = ref(false);
const dialogVisible = ref(false); const error = ref(''); const documentError = ref(''); const documentTip = ref(''); const searchError = ref(''); const searchCompleted = ref(false);
const documentInput = ref(null); const pendingDocument = ref(null); const selectedSource = ref(null); const page = reactive({ total: 0 });
const query = reactive({ page: 1, size: 10, keyword: '', category: '', status: '' });
const form = reactive({ id: null, title: '', summary: '', content: '', category: 'GENERAL', tagsText: '' });
const searchForm = reactive({ query: '', sourceType: '', category: '', topK: 5 });
const categoryOptions = [{ label: '通用咨询 (GENERAL)', value: 'GENERAL' }, { label: '订单 (ORDER)', value: 'ORDER' }, { label: '支付 (PAYMENT)', value: 'PAYMENT' }, { label: '退款售后 (REFUND)', value: 'REFUND' }, { label: '物流配送 (LOGISTICS)', value: 'LOGISTICS' }, { label: '账号安全 (ACCOUNT)', value: 'ACCOUNT' }, { label: '技术问题 (TECHNICAL)', value: 'TECHNICAL' }, { label: '投诉升级 (COMPLAINT)', value: 'COMPLAINT' }];
const categoryFilterOptions = [{ label: '全部分类', value: '' }, ...categoryOptions];
const articleStatusOptions = [{ label: '全部状态', value: '' }, { label: '草稿', value: 'DRAFT' }, { label: '已发布', value: 'PUBLISHED' }, { label: '已下线', value: 'OFFLINE' }];
const sourceTypeOptions = [{ label: '全部来源', value: '' }, { label: '知识文章', value: 'ARTICLE' }, { label: '知识文件', value: 'DOCUMENT' }];

function resetForm() { Object.assign(form, { id: null, title: '', summary: '', content: '', category: 'GENERAL', tagsText: '' }); }
function openCreate() { resetForm(); error.value = ''; dialogVisible.value = true; }
function resetQuery() { Object.assign(query, { page: 1, size: 10, keyword: '', category: '', status: '' }); loadArticles(); }
function changePage(nextPage) { query.page = nextPage; loadArticles(); }
function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-'; }
function statusText(status) { return { DRAFT: '草稿', PUBLISHED: '已发布', OFFLINE: '已下线' }[status] || status || '-'; }
function statusClass(status) { return status === 'PUBLISHED' ? 'published' : status === 'OFFLINE' ? 'off' : 'draft'; }
function documentStatusClass(status) { return status === 'PARSED' ? 'published' : status === 'FAILED' ? 'off' : 'draft'; }
function taskStatusClass(status) { return status === 'INDEXED' || status === 'SUCCESS' ? 'published' : status === 'FAILED' ? 'off' : status === 'PROCESSING' ? 'vip' : 'draft'; }

async function loadArticles() { loading.value = true; try { const data = await knowledgeApi.pageArticles(query); articles.value = data?.records || []; page.total = Number(data?.total || 0); } catch { articles.value = []; page.total = 0; } finally { loading.value = false; } }
async function loadDocuments() { documentsLoading.value = true; try { const data = await knowledgeApi.pageDocuments({ page: 1, size: 50 }); documents.value = data?.records || []; } catch (err) { documentError.value = err.message || '读取知识文件失败'; } finally { documentsLoading.value = false; } }
async function loadIndexTasks() { tasksLoading.value = true; try { const data = await knowledgeApi.pageIndexTasks({ page: 1, size: 50 }); indexTasks.value = data?.records || []; } catch (err) { documentError.value = err.message || '读取索引任务失败'; } finally { tasksLoading.value = false; } }
async function editArticle(id) { error.value = ''; try { const article = await knowledgeApi.getArticle(id); Object.assign(form, { id: article.id, title: article.title || '', summary: article.summary || '', content: article.content || '', category: article.category || 'GENERAL', tagsText: Array.isArray(article.tags) ? article.tags.join(', ') : '' }); dialogVisible.value = true; } catch (err) { error.value = err.message || '读取文章详情失败'; } }
async function saveArticle() { error.value = ''; saving.value = true; const payload = { title: form.title, summary: form.summary || null, content: form.content, category: form.category, tags: form.tagsText.split(',').map((item) => item.trim()).filter(Boolean) }; try { if (form.id) await knowledgeApi.updateArticle(form.id, payload); else await knowledgeApi.createArticle(payload); dialogVisible.value = false; await loadArticles(); } catch (err) { error.value = err.message || '保存知识文章失败'; } finally { saving.value = false; } }
async function publishArticle(article) { if (!confirm(`确认发布知识文章「${article.title}」吗？`)) return; await knowledgeApi.publishArticle(article.id); await loadArticles(); }
async function offlineArticle(article) { if (!confirm(`确认下线知识文章「${article.title}」吗？`)) return; await knowledgeApi.offlineArticle(article.id); await loadArticles(); }
async function deleteArticle(article) { if (!confirm(`确认删除知识文章「${article.title}」吗？`)) return; await knowledgeApi.deleteArticle(article.id); await loadArticles(); }
async function rebuildArticle(article) { await knowledgeApi.rebuildArticleChunks(article.id); await loadChunks('ARTICLE', article.id, article.title); documentTip.value = `已重建「${article.title}」的知识切片`; }
async function createArticleIndex(article) { await knowledgeApi.createArticleIndexTask(article.id); await loadIndexTasks(); documentTip.value = `已创建「${article.title}」的索引任务`; }

function selectDocumentFile(event) { pendingDocument.value = event.target.files?.[0] || null; documentError.value = ''; }
async function importDocument() { if (!pendingDocument.value) return; documentImporting.value = true; documentError.value = ''; documentTip.value = ''; try { const uploaderId = authStore.getUser()?.id; const file = await fileApi.uploadFile(pendingDocument.value, uploaderId); await knowledgeApi.createDocument({ fileId: file.id }); pendingDocument.value = null; if (documentInput.value) documentInput.value.value = ''; documentTip.value = '文件已导入，请执行解析后再重建切片'; await loadDocuments(); } catch (err) { documentError.value = err.message || '导入知识文件失败'; } finally { documentImporting.value = false; } }
async function parseDocument(document) { await knowledgeApi.parseDocument(document.id); documentTip.value = `已完成「${document.documentName}」的文本解析`; await loadDocuments(); }
async function retryParseDocument(document) { await knowledgeApi.retryParseDocument(document.id); documentTip.value = `已重新解析「${document.documentName}」`; await loadDocuments(); }
async function rebuildDocument(document) { await knowledgeApi.rebuildDocumentChunks(document.id); await loadChunks('DOCUMENT', document.id, document.documentName); documentTip.value = `已重建「${document.documentName}」的知识切片`; }
async function createDocumentIndex(document) { await knowledgeApi.createDocumentIndexTask(document.id); await loadIndexTasks(); documentTip.value = `已创建「${document.documentName}」的索引任务`; }
async function offlineDocument(document) { if (!confirm(`确认下线「${document.documentName}」吗？`)) return; await knowledgeApi.offlineDocument(document.id); await loadDocuments(); }
async function deleteDocument(document) { if (!confirm(`确认删除「${document.documentName}」吗？`)) return; await knowledgeApi.deleteDocument(document.id); await loadDocuments(); }

async function loadChunks(sourceType, sourceId, label) { selectedSource.value = { sourceType, sourceId, label }; await reloadSelectedChunks(); }
async function reloadSelectedChunks() { if (!selectedSource.value) return; chunksLoading.value = true; try { const data = await knowledgeApi.pageChunks({ page: 1, size: 100, sourceType: selectedSource.value.sourceType, sourceId: selectedSource.value.sourceId }); chunks.value = data?.records || []; } catch (err) { documentError.value = err.message || '读取知识切片失败'; chunks.value = []; } finally { chunksLoading.value = false; } }
async function executeIndexTask(task) { await knowledgeApi.executeIndexTask(task.id); documentTip.value = `索引任务 #${task.id} 已执行`; await Promise.all([loadIndexTasks(), reloadSelectedChunks()]); }
async function retryIndexTask(task) { await knowledgeApi.retryIndexTask(task.id); documentTip.value = `索引任务 #${task.id} 已恢复为待执行`; await loadIndexTasks(); }
async function runSearch() { searchLoading.value = true; searchError.value = ''; searchCompleted.value = false; try { searchResults.value = await knowledgeApi.search(searchForm) || []; searchCompleted.value = true; } catch (err) { searchResults.value = []; searchError.value = err.message || '语义检索失败'; } finally { searchLoading.value = false; } }

onMounted(() => Promise.all([loadArticles(), loadDocuments(), loadIndexTasks()]));
</script>

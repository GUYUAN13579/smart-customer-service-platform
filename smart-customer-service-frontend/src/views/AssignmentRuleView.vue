<template>
  <section class="page-stack">
    <div class="section-header">
      <div>
        <p class="eyebrow">Dispatch Policy</p>
        <h2>派单规则</h2>
        <p>配置分类、优先级、技能组与权重，后续工单创建时可按规则自动分配。</p>
      </div>
      <button class="primary-button" @click="openCreate">新增规则</button>
    </div>

    <div class="panel">
      <div class="filter-bar">
        <label>规则名称<input v-model.trim="query.keyword" placeholder="搜索规则名称" @keyup.enter="loadRules" /></label>
        <label>分类<input v-model.trim="query.category" placeholder="ACCOUNT / PAYMENT" @keyup.enter="loadRules" /></label>
        <label>优先级<input v-model.trim="query.priority" placeholder="P1 / P2 / P3" @keyup.enter="loadRules" /></label>
        <label>状态
          <AppSelect v-model="query.enabled" :options="enabledFilterOptions" @change="loadRules" />
        </label>
        <div class="filter-actions">
          <button class="ghost-button" @click="resetQuery">重置</button>
          <button class="primary-button" @click="loadRules">查询</button>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>规则名称</th>
              <th>匹配分类</th>
              <th>优先级</th>
              <th>技能组</th>
              <th>权重</th>
              <th>状态</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="rule in rules" :key="rule.id">
              <td class="strong-cell">{{ rule.ruleName }}</td>
              <td>{{ rule.category || '任意分类' }}</td>
              <td>{{ rule.priority || '任意优先级' }}</td>
              <td>#{{ rule.skillGroupId }}</td>
              <td>{{ rule.ruleWeight }}</td>
              <td><span class="status-pill" :class="{ off: rule.enabled === 0 }">{{ rule.enabled === 1 ? '启用' : '停用' }}</span></td>
              <td>{{ formatTime(rule.createdAt) }}</td>
              <td class="actions">
                <button @click="edit(rule)">编辑</button>
                <button class="danger-button" @click="remove(rule)">删除</button>
              </td>
            </tr>
            <tr v-if="!loading && !rules.length">
              <td colspan="8" class="empty-cell">暂无派单规则</td>
            </tr>
            <tr v-if="loading">
              <td colspan="8" class="empty-cell">正在加载规则...</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="pager">
        <span>共 {{ page.total }} 条</span>
        <button class="ghost-button" :disabled="query.page <= 1" @click="changePage(query.page - 1)">上一页</button>
        <span>第 {{ query.page }} 页</span>
        <button class="ghost-button" :disabled="query.page * query.size >= page.total" @click="changePage(query.page + 1)">下一页</button>
      </div>
    </div>

    <div v-if="dialogVisible" class="modal-backdrop" @click.self="dialogVisible = false">
      <form class="modal" @submit.prevent="save">
        <div class="modal-head">
          <div>
            <p class="eyebrow">{{ form.id ? 'Edit Rule' : 'New Rule' }}</p>
            <h3>{{ form.id ? '编辑派单规则' : '新增派单规则' }}</h3>
          </div>
          <button class="icon-button" type="button" @click="dialogVisible = false">×</button>
        </div>
        <label>规则名称<input v-model.trim="form.ruleName" required placeholder="P1 账号问题分配到账号技能组" /></label>
        <div class="form-grid compact-grid">
          <label>分类<input v-model.trim="form.category" placeholder="ACCOUNT" /></label>
          <label>优先级<input v-model.trim="form.priority" placeholder="P1" /></label>
        </div>
        <div class="form-grid compact-grid">
          <label>技能组ID<input v-model.number="form.skillGroupId" required type="number" min="1" /></label>
          <label>权重<input v-model.number="form.ruleWeight" type="number" min="0" /></label>
        </div>
        <label>状态
          <AppSelect v-model="form.enabled" :options="enabledOptions" />
        </label>
        <p v-if="error" class="error-text">{{ error }}</p>
        <div class="modal-actions">
          <button class="ghost-button" type="button" @click="dialogVisible = false">取消</button>
          <button class="primary-button" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </form>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppSelect from '../components/AppSelect.vue';
import { assignmentRuleApi } from '../api/assignmentRules';

const rules = ref([]);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const error = ref('');
const page = reactive({ total: 0 });
const query = reactive({ page: 1, size: 10, keyword: '', category: '', priority: '', enabled: '' });
const enabledFilterOptions = [
  { label: '全部状态', value: '' },
  { label: '启用', value: '1' },
  { label: '停用', value: '0' }
];
const enabledOptions = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 }
];
const form = reactive({ id: null, ruleName: '', category: '', priority: '', skillGroupId: null, ruleWeight: 0, enabled: 1 });

function resetForm() {
  Object.assign(form, { id: null, ruleName: '', category: '', priority: '', skillGroupId: null, ruleWeight: 0, enabled: 1 });
}

function resetQuery() {
  Object.assign(query, { page: 1, size: 10, keyword: '', category: '', priority: '', enabled: '' });
  loadRules();
}

function openCreate() {
  resetForm();
  error.value = '';
  dialogVisible.value = true;
}

function edit(rule) {
  Object.assign(form, rule);
  error.value = '';
  dialogVisible.value = true;
}

function changePage(nextPage) {
  query.page = nextPage;
  loadRules();
}

async function loadRules() {
  loading.value = true;
  try {
    const params = { ...query, enabled: query.enabled === '' ? undefined : Number(query.enabled) };
    const data = await assignmentRuleApi.page(params);
    rules.value = data?.records || [];
    page.total = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

async function save() {
  error.value = '';
  saving.value = true;
  try {
    if (form.id) {
      await assignmentRuleApi.update(form.id, form);
    } else {
      await assignmentRuleApi.create(form);
    }
    dialogVisible.value = false;
    await loadRules();
  } catch (err) {
    error.value = err.message || '保存失败';
  } finally {
    saving.value = false;
  }
}

async function remove(rule) {
  if (!confirm(`确认删除派单规则「${rule.ruleName}」吗？`)) return;
  await assignmentRuleApi.remove(rule.id);
  await loadRules();
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

onMounted(loadRules);
</script>

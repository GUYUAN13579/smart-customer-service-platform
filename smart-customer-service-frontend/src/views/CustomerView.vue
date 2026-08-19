<template>
  <section class="page-stack">
    <div class="section-header">
      <div>
        <p class="eyebrow">Customer Profile</p>
        <h2>客户资料管理</h2>
        <p>维护客户基础信息，为会话、AI 草稿和工单创建提供统一客户档案。</p>
      </div>
      <button class="primary-button" @click="openCreate">新增客户</button>
    </div>

    <div class="panel">
      <div class="filter-bar">
        <label>
          关键词
          <input v-model.trim="query.keyword" placeholder="客户编号 / 名称 / 手机 / 邮箱" @keyup.enter="loadCustomers" />
        </label>
        <label>
          客户等级
          <AppSelect v-model="query.level" :options="levelFilterOptions" @change="loadCustomers" />
        </label>
        <div class="filter-actions">
          <button class="ghost-button" @click="resetQuery">重置</button>
          <button class="primary-button" @click="loadCustomers">查询</button>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>客户编号</th>
              <th>客户名称</th>
              <th>联系方式</th>
              <th>等级</th>
              <th>标签</th>
              <th>更新时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="customer in customers" :key="customer.id">
              <td class="strong-cell">{{ customer.customerNo }}</td>
              <td>
                <strong>{{ customer.name }}</strong>
                <small>{{ customer.remark || '暂无备注' }}</small>
              </td>
              <td>
                <div>{{ customer.phone || '-' }}</div>
                <small>{{ customer.email || '-' }}</small>
              </td>
              <td><span class="status-pill" :class="levelClass(customer.level)">{{ customer.level }}</span></td>
              <td>{{ customer.tags || '-' }}</td>
              <td>{{ formatTime(customer.updatedAt || customer.createdAt) }}</td>
              <td class="actions">
                <button @click="viewDetail(customer.id)">详情</button>
                <button @click="edit(customer)">编辑</button>
                <button class="danger-button" @click="remove(customer)">删除</button>
              </td>
            </tr>
            <tr v-if="!loading && !customers.length">
              <td colspan="7" class="empty-cell">暂无客户数据</td>
            </tr>
            <tr v-if="loading">
              <td colspan="7" class="empty-cell">正在加载客户列表...</td>
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

    <div v-if="dialogVisible" class="modal-backdrop" @click.self="closeDialog">
      <form class="modal large-modal" @submit.prevent="save">
        <div class="modal-head">
          <div>
            <p class="eyebrow">{{ form.id ? 'Edit Customer' : 'New Customer' }}</p>
            <h3>{{ form.id ? '编辑客户资料' : '新增客户资料' }}</h3>
          </div>
          <button class="icon-button" type="button" @click="closeDialog">×</button>
        </div>
        <div class="form-grid">
          <label>客户编号<input v-model.trim="form.customerNo" placeholder="不填则后端自动生成" /></label>
          <label>客户名称<input v-model.trim="form.name" required placeholder="例如：上海星河科技" /></label>
          <label>客户等级
            <AppSelect v-model="form.level" :options="levelOptions" />
          </label>
          <label>手机号<input v-model.trim="form.phone" placeholder="13800000000" /></label>
          <label>邮箱<input v-model.trim="form.email" placeholder="customer@example.com" /></label>
          <label>标签<input v-model.trim="form.tags" placeholder="售后,高价值,企业客户" /></label>
        </div>
        <label>备注<textarea v-model.trim="form.remark" rows="4" placeholder="记录客户偏好、历史问题或服务注意事项"></textarea></label>
        <p v-if="error" class="error-text">{{ error }}</p>
        <div class="modal-actions">
          <button class="ghost-button" type="button" @click="closeDialog">取消</button>
          <button class="primary-button" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
        </div>
      </form>
    </div>

    <div v-if="detailVisible" class="modal-backdrop" @click.self="detailVisible = false">
      <div class="modal">
        <div class="modal-head">
          <div>
            <p class="eyebrow">Customer Detail</p>
            <h3>{{ detail?.name || '客户详情' }}</h3>
          </div>
          <button class="icon-button" type="button" @click="detailVisible = false">×</button>
        </div>
        <dl class="detail-list">
          <div><dt>客户编号</dt><dd>{{ detail?.customerNo }}</dd></div>
          <div><dt>等级</dt><dd>{{ detail?.level }}</dd></div>
          <div><dt>手机号</dt><dd>{{ detail?.phone || '-' }}</dd></div>
          <div><dt>邮箱</dt><dd>{{ detail?.email || '-' }}</dd></div>
          <div><dt>标签</dt><dd>{{ detail?.tags || '-' }}</dd></div>
          <div><dt>备注</dt><dd>{{ detail?.remark || '-' }}</dd></div>
        </dl>
      </div>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import AppSelect from '../components/AppSelect.vue';
import { customerApi } from '../api/customers';

const customers = ref([]);
const detail = ref(null);
const loading = ref(false);
const saving = ref(false);
const dialogVisible = ref(false);
const detailVisible = ref(false);
const error = ref('');
const page = reactive({ total: 0 });
const query = reactive({ page: 1, size: 10, keyword: '', level: '' });
const levelFilterOptions = [
  { label: '全部等级', value: '' },
  { label: '普通客户 NORMAL', value: 'NORMAL' },
  { label: '重点客户 VIP', value: 'VIP' },
  { label: '企业客户 ENTERPRISE', value: 'ENTERPRISE' }
];
const levelOptions = levelFilterOptions.filter((option) => option.value !== '');
const form = reactive({
  id: null,
  customerNo: '',
  name: '',
  phone: '',
  email: '',
  level: 'NORMAL',
  tags: '',
  remark: ''
});

function resetForm() {
  Object.assign(form, { id: null, customerNo: '', name: '', phone: '', email: '', level: 'NORMAL', tags: '', remark: '' });
}

function openCreate() {
  resetForm();
  error.value = '';
  dialogVisible.value = true;
}

function edit(customer) {
  Object.assign(form, customer);
  error.value = '';
  dialogVisible.value = true;
}

function closeDialog() {
  dialogVisible.value = false;
}

function resetQuery() {
  Object.assign(query, { page: 1, size: 10, keyword: '', level: '' });
  loadCustomers();
}

function changePage(nextPage) {
  query.page = nextPage;
  loadCustomers();
}

async function loadCustomers() {
  loading.value = true;
  try {
    const data = await customerApi.page(query);
    customers.value = data?.records || [];
    page.total = data?.total || 0;
  } finally {
    loading.value = false;
  }
}

async function save() {
  error.value = '';
  saving.value = true;
  try {
    const payload = { ...form };
    if (form.id) {
      await customerApi.update(form.id, payload);
    } else {
      await customerApi.create(payload);
    }
    dialogVisible.value = false;
    await loadCustomers();
  } catch (err) {
    error.value = err.message || '保存失败';
  } finally {
    saving.value = false;
  }
}

async function viewDetail(id) {
  detail.value = await customerApi.detail(id);
  detailVisible.value = true;
}

async function remove(customer) {
  if (!confirm(`确认删除客户「${customer.name}」吗？`)) return;
  await customerApi.remove(customer.id);
  await loadCustomers();
}

function levelClass(level) {
  return {
    vip: level === 'VIP',
    enterprise: level === 'ENTERPRISE'
  };
}

function formatTime(value) {
  if (!value) return '-';
  return String(value).replace('T', ' ').slice(0, 16);
}

onMounted(loadCustomers);
</script>

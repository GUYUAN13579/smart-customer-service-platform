<template>
  <section class="page-stack skill-group-page">
    <div class="section-header">
      <div>
        <p class="eyebrow">Dispatch Capacity</p>
        <h2>技能组管理</h2>
        <p>配置工单分类对应的服务能力，并维护可参与自动派单的客服成员与容量。</p>
      </div>
      <button class="primary-button" @click="openGroupModal()">新增技能组</button>
    </div>

    <div class="workbench-grid skill-group-workbench">
      <section class="panel">
        <div class="filter-bar skill-group-filter">
          <label>关键词<input v-model.trim="groupQuery.keyword" placeholder="技能组名称" @keyup.enter="searchGroups" /></label>
          <label>分类<input v-model.trim="groupQuery.category" placeholder="例如 PAYMENT" @keyup.enter="searchGroups" /></label>
          <label>状态<AppSelect v-model="groupQuery.status" :options="statusFilterOptions" @change="searchGroups" /></label>
          <div class="filter-actions">
            <button class="ghost-button" @click="resetGroupFilters">重置</button>
            <button class="primary-button" @click="searchGroups">查询</button>
          </div>
        </div>

        <div class="skill-group-list">
          <article v-for="group in groups" :key="group.id" :class="['skill-group-card', selectedGroup?.id === group.id ? 'active' : '']" @click="selectGroup(group)">
            <div>
              <strong>{{ group.groupName }}</strong>
              <small>分类：{{ group.category || '通用' }} · 创建于 {{ formatTime(group.createdAt) }}</small>
            </div>
            <div class="skill-group-card-actions">
              <span class="status-pill" :class="{ off: group.status === 0 }">{{ group.status === 1 ? '启用' : '停用' }}</span>
              <button class="icon-button" type="button" title="编辑技能组" @click.stop="openGroupModal(group)">✎</button>
              <button class="icon-button danger-icon" type="button" title="删除技能组" @click.stop="removeGroup(group)">×</button>
            </div>
          </article>
          <div v-if="!groupLoading && !groups.length" class="empty-state"><strong>暂无技能组</strong><span>先建立技能组，再添加客服成员。</span></div>
          <div v-if="groupLoading" class="empty-state"><strong>正在加载技能组...</strong></div>
        </div>
        <div class="pager">
          <span>共 {{ groupPage.total }} 条</span>
          <button class="ghost-button" :disabled="groupQuery.page <= 1" @click="changeGroupPage(groupQuery.page - 1)">上一页</button>
          <button class="ghost-button" :disabled="groupQuery.page >= groupTotalPages" @click="changeGroupPage(groupQuery.page + 1)">下一页</button>
        </div>
      </section>

      <aside class="panel detail-panel">
        <template v-if="selectedGroup">
          <div class="panel-title compact-title">
            <div><p class="eyebrow">Group Members</p><h3>{{ selectedGroup.groupName }}</h3><p>{{ selectedGroup.category || '通用分类' }}</p></div>
            <button class="primary-button" @click="openMemberModal()">添加成员</button>
          </div>
          <div class="member-toolbar"><AppSelect v-model="memberQuery.status" :options="statusFilterOptions" @change="loadMembers" /><span>{{ memberPage.total }} 位成员</span></div>
          <div class="member-list">
            <article v-for="member in members" :key="member.id" class="member-item">
              <div><strong>{{ member.realName || member.username || `客服 #${member.userId}` }}</strong><small>用户ID {{ member.userId }} · {{ member.username || '-' }}</small></div>
              <div class="member-capacity"><span>{{ member.maxActiveTickets }} 单上限</span><span class="status-pill" :class="{ off: member.status === 0 }">{{ member.status === 1 ? '启用' : '停用' }}</span><button class="icon-button" type="button" title="编辑成员" @click="openMemberModal(member)">✎</button><button class="icon-button danger-icon" type="button" title="移除成员" @click="removeMember(member)">×</button></div>
            </article>
            <div v-if="!memberLoading && !members.length" class="empty-state small-empty"><strong>暂无成员</strong><span>添加启用的客服账号后可参与自动派单。</span></div>
            <div v-if="memberLoading" class="empty-state small-empty"><strong>正在加载成员...</strong></div>
          </div>
          <div class="pager"><span>第 {{ memberQuery.page }} 页 / 共 {{ memberTotalPages }} 页</span><button class="ghost-button" :disabled="memberQuery.page <= 1" @click="changeMemberPage(memberQuery.page - 1)">上一页</button><button class="ghost-button" :disabled="memberQuery.page >= memberTotalPages" @click="changeMemberPage(memberQuery.page + 1)">下一页</button></div>
        </template>
        <div v-else class="empty-state"><strong>选择一个技能组</strong><span>右侧会展示成员配置与最大活跃工单容量。</span></div>
        <p v-if="error" class="error-text">{{ error }}</p>
      </aside>
    </div>

    <div v-if="groupModalVisible" class="modal-backdrop" @click.self="groupModalVisible = false"><form class="modal" @submit.prevent="saveGroup"><div class="modal-head"><div><p class="eyebrow">{{ groupForm.id ? 'Edit Skill Group' : 'New Skill Group' }}</p><h3>{{ groupForm.id ? '编辑技能组' : '新增技能组' }}</h3></div><button class="icon-button" type="button" @click="groupModalVisible = false">×</button></div><label>技能组名称<input v-model.trim="groupForm.groupName" required placeholder="例如：支付与退款组" /></label><label>工单分类<input v-model.trim="groupForm.category" placeholder="例如 PAYMENT，可留空表示通用" /></label><label>状态<AppSelect v-model="groupForm.status" :options="statusOptions" /></label><div class="modal-actions"><button class="ghost-button" type="button" @click="groupModalVisible = false">取消</button><button class="primary-button" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button></div></form></div>
    <div v-if="memberModalVisible" class="modal-backdrop" @click.self="memberModalVisible = false"><form class="modal" @submit.prevent="saveMember"><div class="modal-head"><div><p class="eyebrow">{{ memberForm.id ? 'Edit Member' : 'Add Member' }}</p><h3>{{ memberForm.id ? '编辑成员配置' : '添加客服成员' }}</h3></div><button class="icon-button" type="button" @click="memberModalVisible = false">×</button></div><label v-if="!memberForm.id">客服用户ID<input v-model.number="memberForm.userId" required type="number" min="1" placeholder="输入 AGENT 用户 ID" /></label><label>最大活跃工单数<input v-model.number="memberForm.maxActiveTickets" required type="number" min="1" max="1000" /></label><label>状态<AppSelect v-model="memberForm.status" :options="statusOptions" /></label><div class="modal-actions"><button class="ghost-button" type="button" @click="memberModalVisible = false">取消</button><button class="primary-button" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button></div></form></div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue';
import AppSelect from '../components/AppSelect.vue';
import { skillGroupApi } from '../api/skillGroups';

const groups = ref([]); const members = ref([]); const selectedGroup = ref(null); const groupLoading = ref(false); const memberLoading = ref(false); const saving = ref(false); const error = ref(''); const groupModalVisible = ref(false); const memberModalVisible = ref(false);
const groupPage = reactive({ total: 0 }); const memberPage = reactive({ total: 0 });
const groupQuery = reactive({ page: 1, size: 10, keyword: '', category: '', status: '' }); const memberQuery = reactive({ page: 1, size: 10, status: '' });
const groupForm = reactive({ id: null, groupName: '', category: '', status: 1 }); const memberForm = reactive({ id: null, userId: null, maxActiveTickets: 20, status: 1 });
const statusOptions = [{ label: '启用', value: 1 }, { label: '停用', value: 0 }]; const statusFilterOptions = [{ label: '全部状态', value: '' }, ...statusOptions];
const groupTotalPages = computed(() => Math.max(1, Math.ceil(groupPage.total / groupQuery.size))); const memberTotalPages = computed(() => Math.max(1, Math.ceil(memberPage.total / memberQuery.size)));
async function loadGroups() { groupLoading.value = true; error.value = ''; try { const data = await skillGroupApi.page({ ...groupQuery, status: groupQuery.status === '' ? undefined : Number(groupQuery.status) }); groups.value = data?.records || []; groupPage.total = data?.total || 0; const next = groups.value.find((item) => item.id === selectedGroup.value?.id) || groups.value[0]; if (next) await selectGroup(next); else { selectedGroup.value = null; members.value = []; memberPage.total = 0; } } catch (err) { error.value = err.message || '加载技能组失败'; } finally { groupLoading.value = false; } }
async function selectGroup(group) { selectedGroup.value = group; memberQuery.page = 1; await loadMembers(); }
async function loadMembers() { if (!selectedGroup.value) return; memberLoading.value = true; try { const data = await skillGroupApi.members(selectedGroup.value.id, { ...memberQuery, status: memberQuery.status === '' ? undefined : Number(memberQuery.status) }); members.value = data?.records || []; memberPage.total = data?.total || 0; } catch (err) { error.value = err.message || '加载技能组成员失败'; } finally { memberLoading.value = false; } }
function openGroupModal(group = null) { Object.assign(groupForm, group ? group : { id: null, groupName: '', category: '', status: 1 }); error.value = ''; groupModalVisible.value = true; }
async function saveGroup() { saving.value = true; error.value = ''; try { const data = { groupName: groupForm.groupName, category: groupForm.category || null, status: Number(groupForm.status) }; if (groupForm.id) await skillGroupApi.update(groupForm.id, data); else await skillGroupApi.create(data); groupModalVisible.value = false; await loadGroups(); } catch (err) { error.value = err.message || '保存技能组失败'; } finally { saving.value = false; } }
async function removeGroup(group) { if (!confirm(`确认删除技能组「${group.groupName}」吗？`)) return; try { await skillGroupApi.remove(group.id); if (selectedGroup.value?.id === group.id) selectedGroup.value = null; await loadGroups(); } catch (err) { error.value = err.message || '删除技能组失败'; } }
function openMemberModal(member = null) { Object.assign(memberForm, member ? { id: member.id, userId: member.userId, maxActiveTickets: member.maxActiveTickets, status: member.status } : { id: null, userId: null, maxActiveTickets: 20, status: 1 }); error.value = ''; memberModalVisible.value = true; }
async function saveMember() { if (!selectedGroup.value) return; saving.value = true; error.value = ''; try { const data = { maxActiveTickets: Number(memberForm.maxActiveTickets), status: Number(memberForm.status) }; if (memberForm.id) await skillGroupApi.updateMember(selectedGroup.value.id, memberForm.id, data); else await skillGroupApi.createMember(selectedGroup.value.id, { ...data, userId: memberForm.userId }); memberModalVisible.value = false; await loadMembers(); } catch (err) { error.value = err.message || '保存技能组成员失败'; } finally { saving.value = false; } }
async function removeMember(member) { if (!confirm(`确认从技能组移除「${member.realName || member.username || member.userId}」吗？`)) return; try { await skillGroupApi.removeMember(selectedGroup.value.id, member.id); await loadMembers(); } catch (err) { error.value = err.message || '移除成员失败'; } }
function searchGroups() { groupQuery.page = 1; loadGroups(); } function resetGroupFilters() { Object.assign(groupQuery, { page: 1, size: 10, keyword: '', category: '', status: '' }); loadGroups(); } function changeGroupPage(page) { groupQuery.page = page; loadGroups(); } function changeMemberPage(page) { memberQuery.page = page; loadMembers(); } function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '-'; }
onMounted(loadGroups);
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-mark">CS</div>
        <div>
          <strong>智能客服平台</strong>
          <span>工单自动化后台</span>
        </div>
      </div>

      <nav class="nav">
        <RouterLink v-for="item in navItems" :key="item.path" :to="item.path">
          <span class="nav-icon">{{ item.icon }}</span>
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <p class="eyebrow">Customer Service Console</p>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="user-box">
          <div>
            <strong>{{ currentUser?.realName || currentUser?.username || '未命名用户' }}</strong>
            <span>{{ roleText }}</span>
          </div>
          <button class="ghost-button" @click="logout">退出</button>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authApi } from '../api/auth';
import { authStore } from '../stores/authStore';

const route = useRoute();
const router = useRouter();
const currentUser = ref(authStore.getUser());

const navItems = [
  { path: '/dashboard', label: '运营看板', icon: '⌂' },
  { path: '/tickets', label: '工单中心', icon: '▤' },
  { path: '/conversations', label: '会话中心', icon: '◫' },
  { path: '/customers', label: '客户管理', icon: '◎' },
  { path: '/assignment-rules', label: '派单规则', icon: '↗' },
  { path: '/ai-workbench', label: 'AI 工作台', icon: '✦' },
  { path: '/knowledge', label: '知识库', icon: '▣' }
];

const currentTitle = computed(() => navItems.find((item) => item.path === route.path)?.label || '工作台');
const roleText = computed(() => {
  const roles = currentUser.value?.roleCodes;
  if (Array.isArray(roles)) return roles.join(' / ') || '暂无角色';
  return roles || '暂无角色';
});

async function logout() {
  try {
    await authApi.logout();
  } catch {
    // 即使后端登出失败，也清理本地登录态。
  }
  authStore.clear();
  router.push('/login');
}

onMounted(async () => {
  try {
    const user = await authApi.me();
    authStore.saveUser(user);
    currentUser.value = user;
  } catch {
    currentUser.value = authStore.getUser();
  }
});
</script>

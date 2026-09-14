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
        <div class="topbar-actions">
          <RouterLink class="notification-entry" to="/notifications" aria-label="通知中心">
            <span class="notification-entry-icon">通知</span>
            <span v-if="unreadCount > 0" class="notification-badge">{{ unreadBadge }}</span>
          </RouterLink>
          <div class="user-box">
          <div>
            <strong>{{ currentUser?.realName || currentUser?.username || '未命名用户' }}</strong>
            <span>{{ roleText }}</span>
          </div>
          <button class="ghost-button" @click="logout">退出</button>
          </div>
        </div>
      </header>
      <RouterView />
    </main>
  </div>
</template>

<script setup>
// DashboardLayout.vue 提供管理端共享布局。
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authApi } from '../api/auth';
import { authStore } from '../stores/authStore';
import { notificationApi } from '../api/notifications';

const route = useRoute();
const router = useRouter();
const currentUser = ref(authStore.getUser());
const unreadCount = ref(0);
let unreadTimer = null;

const navItems = computed(() => [
  { path: '/dashboard', label: '运营看板', icon: '⌂' },
  { path: '/tickets', label: '工单中心', icon: '▤' },
  { path: '/conversations', label: '会话中心', icon: '◫' },
  { path: '/notifications', label: '通知中心', icon: '●' },
  { path: '/skill-groups', label: '技能组管理', icon: '◇', roles: ['ADMIN', 'SUPERVISOR'] },
  { path: '/customers', label: '客户管理', icon: '◎' },
  { path: '/assignment-rules', label: '派单规则', icon: '↗' },
  { path: '/ai-workbench', label: 'AI 工作台', icon: '✦' },
  { path: '/knowledge', label: '知识库', icon: '▣', roles: ['ADMIN', 'SUPERVISOR'] }
].filter((item) => !item.roles || item.roles.some((role) => currentUser.value?.roleCodes?.includes?.(role))));

const currentTitle = computed(() => navItems.value.find((item) => item.path === route.path)?.label || '工作台');
const roleText = computed(() => {
  const roles = currentUser.value?.roleCodes;
  if (Array.isArray(roles)) return roles.join(' / ') || '暂无角色';
  return roles || '暂无角色';
});
const unreadBadge = computed(() => unreadCount.value > 99 ? '99+' : unreadCount.value);

async function loadUnreadCount() {
  try {
    const data = await notificationApi.unreadCount();
    unreadCount.value = Number(data?.unreadCount || 0);
  } catch {
    // 通知接口暂不可用时不影响主控制台继续使用。
  }
}

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
  await loadUnreadCount();
  window.addEventListener('notification-updated', loadUnreadCount);
  unreadTimer = window.setInterval(loadUnreadCount, 30000);
});

onUnmounted(() => {
  window.removeEventListener('notification-updated', loadUnreadCount);
  if (unreadTimer) window.clearInterval(unreadTimer);
});
</script>

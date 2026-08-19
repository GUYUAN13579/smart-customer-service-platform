<template>
  <section class="auth-page">
    <div class="auth-panel">
      <div class="auth-copy">
        <p class="eyebrow">Smart Customer Service</p>
        <h1>智能客服与工单自动化平台</h1>
        <p>面向咨询、报障、售后的客服工作台，串联 AI 问答、知识检索、工单流转与 SLA 提醒。</p>
      </div>

      <form class="auth-card" @submit.prevent="submit">
        <h2>登录后台</h2>
        <label>
          用户名
          <input v-model.trim="form.username" placeholder="agent01" autocomplete="username" />
        </label>
        <label>
          密码
          <input v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </label>
        <p v-if="error" class="error-text">{{ error }}</p>
        <button class="primary-button" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
        <RouterLink class="text-link" to="/register">没有账号，去注册</RouterLink>
      </form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { authApi } from '../api/auth';
import { authStore } from '../stores/authStore';

const router = useRouter();
const route = useRoute();
const loading = ref(false);
const error = ref('');
const form = reactive({ username: '', password: '' });

async function submit() {
  error.value = '';
  loading.value = true;
  try {
    const data = await authApi.login(form);
    authStore.saveLogin(data);
    router.push(route.query.redirect || '/dashboard');
  } catch (err) {
    error.value = err.message || '登录失败';
  } finally {
    loading.value = false;
  }
}
</script>

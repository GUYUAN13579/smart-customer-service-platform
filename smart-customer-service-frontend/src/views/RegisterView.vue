<template>
  <section class="auth-page">
    <div class="auth-panel compact">
      <form class="auth-card wide" @submit.prevent="submit">
        <p class="eyebrow">Create Account</p>
        <h2>注册客服账号</h2>
        <div class="form-grid">
          <label>用户名<input v-model.trim="form.username" placeholder="agent01" /></label>
          <label>真实姓名<input v-model.trim="form.realName" placeholder="张三" /></label>
          <label>密码<input v-model="form.password" type="password" placeholder="至少 6 位" /></label>
          <label>部门<input v-model.trim="form.department" placeholder="客服中心" /></label>
          <label>手机号<input v-model.trim="form.phone" placeholder="13800000000" /></label>
          <label>邮箱<input v-model.trim="form.email" placeholder="agent@example.com" /></label>
        </div>
        <p v-if="message" class="success-text">{{ message }}</p>
        <p v-if="error" class="error-text">{{ error }}</p>
        <button class="primary-button" :disabled="loading">{{ loading ? '提交中...' : '注册' }}</button>
        <RouterLink class="text-link" to="/login">已有账号，返回登录</RouterLink>
      </form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { authApi } from '../api/auth';

const loading = ref(false);
const error = ref('');
const message = ref('');
const form = reactive({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  department: ''
});

async function submit() {
  error.value = '';
  message.value = '';
  loading.value = true;
  try {
    const user = await authApi.register(form);
    message.value = `注册成功：${user.username}`;
  } catch (err) {
    error.value = err.message || '注册失败';
  } finally {
    loading.value = false;
  }
}
</script>

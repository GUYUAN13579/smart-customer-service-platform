// main.js 属于 Vue 前端应用。
import { createApp } from 'vue';
import App from './App.vue';
import router from './router';
import './styles/main.css';

createApp(App).use(router).mount('#app');

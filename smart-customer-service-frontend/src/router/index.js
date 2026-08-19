import { createRouter, createWebHistory } from 'vue-router';
import { authStore } from '../stores/authStore';
import LoginView from '../views/LoginView.vue';
import RegisterView from '../views/RegisterView.vue';
import DashboardLayout from '../layouts/DashboardLayout.vue';
import DashboardView from '../views/DashboardView.vue';
import TicketView from '../views/TicketView.vue';
import AssignmentRuleView from '../views/AssignmentRuleView.vue';
import KnowledgeView from '../views/KnowledgeView.vue';
import AiWorkbenchView from '../views/AiWorkbenchView.vue';
import CustomerView from '../views/CustomerView.vue';
import ConversationView from '../views/ConversationView.vue';
import CustomerChatTestView from '../views/CustomerChatTestView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/dashboard' },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/customer-chat', name: 'customer-chat', component: CustomerChatTestView },
    {
      path: '/',
      component: DashboardLayout,
      meta: { requiresAuth: true },
      children: [
        { path: 'dashboard', name: 'dashboard', component: DashboardView },
        { path: 'tickets', name: 'tickets', component: TicketView },
        { path: 'assignment-rules', name: 'assignment-rules', component: AssignmentRuleView },
        { path: 'knowledge', name: 'knowledge', component: KnowledgeView },
        { path: 'ai-workbench', name: 'ai-workbench', component: AiWorkbenchView },
        { path: 'conversations', name: 'conversations', component: ConversationView },
        { path: 'customers', name: 'customers', component: CustomerView }
      ]
    }
  ]
});

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !authStore.isLoggedIn()) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if ((to.name === 'login' || to.name === 'register') && authStore.isLoggedIn()) {
    return { name: 'dashboard' };
  }
  return true;
});

export default router;

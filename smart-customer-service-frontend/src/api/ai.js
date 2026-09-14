// ai.js 集中封装当前后端模块的前端请求。
import { request } from './request';

export const aiApi = {
  chat(data) {
    return request('/api/v1/ai/chat', { method: 'POST', body: data });
  },
  autoReply(sessionId, data = {}) {
    return request(`/api/v1/ai/conversations/${sessionId}/auto-reply`, { method: 'POST', body: data });
  },
  ticketDraft(sessionId, data = {}) {
    return request(`/api/v1/ai/conversations/${sessionId}/ticket-draft`, { method: 'POST', body: data });
  }
};

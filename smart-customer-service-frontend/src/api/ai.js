import { request } from './request';

export const aiApi = {
  chat(data) {
    return request('/api/v1/ai/chat', { method: 'POST', body: data });
  },
  ticketDraft(data) {
    return request('/api/v1/ai/ticket-draft', { method: 'POST', body: data });
  }
};

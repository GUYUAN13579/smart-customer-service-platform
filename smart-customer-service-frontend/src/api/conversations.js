import { request, toQuery } from './request';

export const conversationApi = {
  page(params) {
    return request(`/api/v1/conversations${toQuery(params)}`);
  },
  create(data) {
    return request('/api/v1/conversations', { method: 'POST', body: data });
  },
  detail(id) {
    return request(`/api/v1/conversations/${id}`);
  },
  messages(id) {
    return request(`/api/v1/conversations/${id}/messages`);
  },
  sendMessage(id, data) {
    return request(`/api/v1/conversations/${id}/messages`, { method: 'POST', body: data });
  },
  takeOver(id, data = {}) {
    return request(`/api/v1/conversations/${id}/take-over`, { method: 'POST', body: data });
  },
  releaseTakeOver(id) {
    return request(`/api/v1/conversations/${id}/release-take-over`, { method: 'POST' });
  },
  close(id, data = {}) {
    return request(`/api/v1/conversations/${id}/close`, { method: 'POST', body: data });
  }
};

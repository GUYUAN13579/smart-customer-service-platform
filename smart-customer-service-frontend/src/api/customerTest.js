// customerTest.js 集中封装当前后端模块的前端请求。
import { request } from './request';

function buildForm(file, uploaderId) {
  const form = new FormData();
  form.append('file', file);
  if (uploaderId !== undefined && uploaderId !== null && uploaderId !== '') {
    form.append('uploaderId', uploaderId);
  }
  return form;
}

export const customerTestApi = {
  createSession(data) {
    return request('/api/v1/customer-test/conversations', { method: 'POST', body: data });
  },
  detail(id) {
    return request(`/api/v1/customer-test/conversations/${id}`);
  },
  messages(id) {
    return request(`/api/v1/customer-test/conversations/${id}/messages`);
  },
  sendMessage(id, data) {
    return request(`/api/v1/customer-test/conversations/${id}/messages`, { method: 'POST', body: data });
  },
  autoReply(id, data = {}) {
    return request(`/api/v1/customer-test/conversations/${id}/auto-reply`, { method: 'POST', body: data });
  },
  ticketDraft(id, data = {}) {
    return request(`/api/v1/customer-test/conversations/${id}/ticket-draft`, { method: 'POST', body: data });
  },
  manualTransfer(id, data = {}) {
    return request(`/api/v1/customer-test/conversations/${id}/manual-transfer`, { method: 'POST', body: data });
  },
  uploadImage(file, uploaderId) {
    return request('/api/v1/customer-test/conversations/files/images/upload', {
      method: 'POST',
      body: buildForm(file, uploaderId)
    });
  },
  uploadFile(file, uploaderId) {
    return request('/api/v1/customer-test/conversations/files/upload', {
      method: 'POST',
      body: buildForm(file, uploaderId)
    });
  }
};

// tickets.js 集中封装当前后端模块的前端请求。
import { request, toQuery } from './request';

export const ticketApi = {
  page(params) {
    return request(`/api/v1/tickets${toQuery(params)}`);
  },
  pendingReview(params) {
    return request(`/api/v1/tickets/pending-review${toQuery(params)}`);
  },
  waitingAssign(params) {
    return request(`/api/v1/tickets/waiting-assign${toQuery(params)}`);
  },
  my(params) {
    return request(`/api/v1/tickets/my${toQuery(params)}`);
  },
  detail(id) {
    return request(`/api/v1/tickets/${id}`);
  },
  fullDetail(id) {
    return request(`/api/v1/tickets/${id}/full-detail`);
  },
  create(data) {
    return request('/api/v1/tickets', { method: 'POST', body: data });
  },
  review(id, data) {
    return request(`/api/v1/tickets/${id}/review`, { method: 'PUT', body: data });
  },
  assign(id, data) {
    return request(`/api/v1/tickets/${id}/assign`, { method: 'PUT', body: data });
  },
  assignmentPreview(id) {
    return request(`/api/v1/tickets/${id}/assignment-preview`);
  },
  autoAssign(id) {
    return request(`/api/v1/tickets/${id}/auto-assign`, { method: 'POST' });
  },
  start(id, data = {}) {
    return request(`/api/v1/tickets/${id}/start`, { method: 'PUT', body: data });
  },
  addRecord(id, data) {
    return request(`/api/v1/tickets/${id}/records`, { method: 'POST', body: data });
  },
  records(id, params) {
    return request(`/api/v1/tickets/${id}/records${toQuery(params)}`);
  },
  operationLogs(id, params) {
    return request(`/api/v1/tickets/${id}/operation-logs${toQuery(params)}`);
  },
  resolve(id, data) {
    return request(`/api/v1/tickets/${id}/resolve`, { method: 'PUT', body: data });
  },
  close(id, data) {
    return request(`/api/v1/tickets/${id}/close`, { method: 'PUT', body: data });
  }
};

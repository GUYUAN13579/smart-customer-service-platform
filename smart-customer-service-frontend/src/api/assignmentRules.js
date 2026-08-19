import { request, toQuery } from './request';

export const assignmentRuleApi = {
  page(params) {
    return request(`/api/v1/assignment-rules${toQuery(params)}`);
  },
  detail(id) {
    return request(`/api/v1/assignment-rules/${id}`);
  },
  create(data) {
    return request('/api/v1/assignment-rules', { method: 'POST', body: data });
  },
  update(id, data) {
    return request(`/api/v1/assignment-rules/${id}`, { method: 'PUT', body: data });
  },
  remove(id) {
    return request(`/api/v1/assignment-rules/${id}`, { method: 'DELETE' });
  }
};

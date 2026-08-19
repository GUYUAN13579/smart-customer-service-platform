import { request, toQuery } from './request';

export const customerApi = {
  page(params) {
    return request(`/api/v1/customers${toQuery(params)}`);
  },
  detail(id) {
    return request(`/api/v1/customers/${id}`);
  },
  create(data) {
    return request('/api/v1/customers', { method: 'POST', body: data });
  },
  update(id, data) {
    return request(`/api/v1/customers/${id}`, { method: 'PUT', body: data });
  },
  remove(id) {
    return request(`/api/v1/customers/${id}`, { method: 'DELETE' });
  }
};

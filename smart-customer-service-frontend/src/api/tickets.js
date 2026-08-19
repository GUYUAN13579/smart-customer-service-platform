import { request, toQuery } from './request';

export const ticketApi = {
  page(params) {
    return request(`/api/v1/tickets${toQuery(params)}`);
  },
  detail(id) {
    return request(`/api/v1/tickets/${id}`);
  },
  create(data) {
    return request('/api/v1/tickets', { method: 'POST', body: data });
  },
  assign(id, data) {
    return request(`/api/v1/tickets/${id}/assign`, { method: 'POST', body: data });
  },
  close(id, data) {
    return request(`/api/v1/tickets/${id}/close`, { method: 'POST', body: data });
  }
};

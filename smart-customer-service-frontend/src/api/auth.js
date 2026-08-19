import { request } from './request';

export const authApi = {
  register(data) {
    return request('/api/v1/auth/register', { method: 'POST', body: data });
  },
  login(data) {
    return request('/api/v1/auth/login', { method: 'POST', body: data });
  },
  refresh(data) {
    return request('/api/v1/auth/refresh', { method: 'POST', body: data });
  },
  logout(data) {
    return request('/api/v1/auth/logout', { method: 'POST', body: data });
  },
  me() {
    return request('/api/v1/users/me');
  },
  menus() {
    return request('/api/v1/menus/me');
  }
};

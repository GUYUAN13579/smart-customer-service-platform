// notifications.js 集中封装站内通知相关请求。
import { request, toQuery } from './request';

export const notificationApi = {
  page(params) {
    return request(`/api/v1/notifications${toQuery(params)}`);
  },
  unreadCount() {
    return request('/api/v1/notifications/unread-count');
  },
  markAsRead(id) {
    return request(`/api/v1/notifications/${id}/read`, { method: 'PUT' });
  },
  markAllAsRead() {
    return request('/api/v1/notifications/read-all', { method: 'PUT' });
  }
};

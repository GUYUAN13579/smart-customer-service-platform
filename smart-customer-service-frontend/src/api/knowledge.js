import { request, toQuery } from './request';

export const knowledgeApi = {
  search(data) {
    return request('/api/v1/knowledge/search', { method: 'POST', body: data });
  },
  pageArticles(params) {
    return request(`/api/v1/knowledge/articles${toQuery(params)}`);
  },
  createArticle(data) {
    return request('/api/v1/knowledge/articles', { method: 'POST', body: data });
  },
  publishArticle(id) {
    return request(`/api/v1/knowledge/articles/${id}/publish`, { method: 'POST' });
  },
  offlineArticle(id) {
    return request(`/api/v1/knowledge/articles/${id}/offline`, { method: 'POST' });
  }
};

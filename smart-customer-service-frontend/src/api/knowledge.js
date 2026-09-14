// knowledge.js 集中封装当前后端模块的前端请求。
import { request, toQuery } from './request';

export const knowledgeApi = {
  pageArticles(params) {
    return request(`/api/v1/knowledge/articles${toQuery(params)}`);
  },
  getArticle(id) {
    return request(`/api/v1/knowledge/articles/${id}`);
  },
  createArticle(data) {
    return request('/api/v1/knowledge/articles', { method: 'POST', body: data });
  },
  updateArticle(id, data) {
    return request(`/api/v1/knowledge/articles/${id}`, { method: 'PUT', body: data });
  },
  deleteArticle(id) {
    return request(`/api/v1/knowledge/articles/${id}`, { method: 'DELETE' });
  },
  publishArticle(id) {
    return request(`/api/v1/knowledge/articles/${id}/publish`, { method: 'POST' });
  },
  offlineArticle(id) {
    return request(`/api/v1/knowledge/articles/${id}/offline`, { method: 'POST' });
  },
  createDocument(data) {
    return request('/api/v1/knowledge/documents', { method: 'POST', body: data });
  },
  pageDocuments(params) {
    return request(`/api/v1/knowledge/documents${toQuery(params)}`);
  },
  parseDocument(id) {
    return request(`/api/v1/knowledge/documents/${id}/parse`, { method: 'POST' });
  },
  retryParseDocument(id) {
    return request(`/api/v1/knowledge/documents/${id}/retry-parse`, { method: 'POST' });
  },
  offlineDocument(id) {
    return request(`/api/v1/knowledge/documents/${id}/offline`, { method: 'POST' });
  },
  deleteDocument(id) {
    return request(`/api/v1/knowledge/documents/${id}`, { method: 'DELETE' });
  },
  rebuildArticleChunks(id) {
    return request(`/api/v1/knowledge/articles/${id}/chunks/rebuild`, { method: 'POST' });
  },
  rebuildDocumentChunks(id) {
    return request(`/api/v1/knowledge/documents/${id}/chunks/rebuild`, { method: 'POST' });
  },
  pageChunks(params) {
    return request(`/api/v1/knowledge/chunks${toQuery(params)}`);
  },
  createArticleIndexTask(id) {
    return request(`/api/v1/knowledge/articles/${id}/index`, { method: 'POST' });
  },
  createDocumentIndexTask(id) {
    return request(`/api/v1/knowledge/documents/${id}/index`, { method: 'POST' });
  },
  pageIndexTasks(params) {
    return request(`/api/v1/knowledge/index-tasks${toQuery(params)}`);
  },
  executeIndexTask(id) {
    return request(`/api/v1/knowledge/index-tasks/${id}/execute`, { method: 'POST' });
  },
  retryIndexTask(id) {
    return request(`/api/v1/knowledge/index-tasks/${id}/retry`, { method: 'POST' });
  },
  search(params) {
    return request(`/api/v1/knowledge/search${toQuery(params)}`);
  }
};

// skillGroups.js 集中封装技能组及其成员管理请求。
import { request, toQuery } from './request';

export const skillGroupApi = {
  page(params) { return request(`/api/v1/skill-groups${toQuery(params)}`); },
  detail(id) { return request(`/api/v1/skill-groups/${id}`); },
  create(data) { return request('/api/v1/skill-groups', { method: 'POST', body: data }); },
  update(id, data) { return request(`/api/v1/skill-groups/${id}`, { method: 'PUT', body: data }); },
  remove(id) { return request(`/api/v1/skill-groups/${id}`, { method: 'DELETE' }); },
  members(skillGroupId, params) { return request(`/api/v1/skill-groups/${skillGroupId}/members${toQuery(params)}`); },
  createMember(skillGroupId, data) { return request(`/api/v1/skill-groups/${skillGroupId}/members`, { method: 'POST', body: data }); },
  updateMember(skillGroupId, memberId, data) { return request(`/api/v1/skill-groups/${skillGroupId}/members/${memberId}`, { method: 'PUT', body: data }); },
  removeMember(skillGroupId, memberId) { return request(`/api/v1/skill-groups/${skillGroupId}/members/${memberId}`, { method: 'DELETE' }); }
};

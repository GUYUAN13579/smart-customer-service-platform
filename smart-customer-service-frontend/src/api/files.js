import { request } from './request';

function buildForm(file, uploaderId) {
  const form = new FormData();
  form.append('file', file);
  if (uploaderId !== undefined && uploaderId !== null && uploaderId !== '') {
    form.append('uploaderId', uploaderId);
  }
  return form;
}

export const fileApi = {
  uploadFile(file, uploaderId) {
    return request('/api/v1/files/upload', {
      method: 'POST',
      body: buildForm(file, uploaderId)
    });
  },
  uploadImage(file, uploaderId) {
    return request('/api/v1/files/images/upload', {
      method: 'POST',
      body: buildForm(file, uploaderId)
    });
  },
  detail(id) {
    return request(`/api/v1/files/${id}`);
  }
};

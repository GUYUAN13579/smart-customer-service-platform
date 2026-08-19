import { authStore } from '../stores/authStore';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

export class ApiError extends Error {
  constructor(message, response) {
    super(message);
    this.name = 'ApiError';
    this.response = response;
  }
}

export async function request(path, options = {}) {
  return doRequest(path, options, true);
}

async function doRequest(path, options = {}, retryWithRefresh) {
  const headers = new Headers(options.headers || {});
  const token = authStore.getAccessToken();
  const isFormData = options.body instanceof FormData;

  if (!headers.has('Content-Type') && options.body !== undefined && !isFormData) {
    headers.set('Content-Type', 'application/json;charset=UTF-8');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers,
    body: options.body === undefined ? undefined : isFormData ? options.body : JSON.stringify(options.body)
  });

  const contentType = response.headers.get('content-type') || '';
  const payload = contentType.includes('application/json') ? await response.json() : await response.text();

  if (response.status === 401 && retryWithRefresh && !path.includes('/api/v1/auth/refresh')) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return doRequest(path, options, false);
    }
  }

  if (!response.ok) {
    throw new ApiError(typeof payload === 'string' ? payload : payload.message || '请求失败', payload);
  }

  if (payload && typeof payload === 'object' && 'code' in payload) {
    if (payload.code !== 0) {
      throw new ApiError(payload.message || '业务处理失败', payload);
    }
    return payload.data;
  }

  return payload;
}

async function refreshAccessToken() {
  const refreshToken = authStore.getRefreshToken();
  if (!refreshToken) {
    authStore.clear();
    return false;
  }

  try {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json;charset=UTF-8' },
      body: JSON.stringify({ refreshToken })
    });
    const payload = await response.json();
    if (!response.ok || payload.code !== 0 || !payload.data?.accessToken) {
      authStore.clear();
      return false;
    }
    authStore.saveAccessToken(payload.data);
    return true;
  } catch {
    authStore.clear();
    return false;
  }
}

export function toQuery(params = {}) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      search.append(key, value);
    }
  });
  const value = search.toString();
  return value ? `?${value}` : '';
}

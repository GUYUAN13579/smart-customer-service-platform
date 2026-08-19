const ACCESS_TOKEN_KEY = 'scs_access_token';
const REFRESH_TOKEN_KEY = 'scs_refresh_token';
const USER_KEY = 'scs_login_user';

export const authStore = {
  getAccessToken() {
    return localStorage.getItem(ACCESS_TOKEN_KEY);
  },
  getRefreshToken() {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
  },
  getUser() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  },
  isLoggedIn() {
    return Boolean(this.getAccessToken());
  },
  saveLogin(loginUser) {
    localStorage.setItem(ACCESS_TOKEN_KEY, loginUser.accessToken || '');
    if (loginUser.refreshToken) {
      localStorage.setItem(REFRESH_TOKEN_KEY, loginUser.refreshToken);
    }
    localStorage.setItem(USER_KEY, JSON.stringify({ ...this.getUser(), ...loginUser }));
  },
  saveAccessToken(tokenInfo) {
    localStorage.setItem(ACCESS_TOKEN_KEY, tokenInfo.accessToken || '');
    localStorage.setItem(USER_KEY, JSON.stringify({ ...this.getUser(), ...tokenInfo }));
  },
  saveUser(user) {
    localStorage.setItem(USER_KEY, JSON.stringify({ ...this.getUser(), ...user }));
  },
  clear() {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
  }
};

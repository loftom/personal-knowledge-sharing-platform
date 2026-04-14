export type AuthPayload = {
  token: string;
  userId: string;
  username: string;
  nickname: string;
  role: string;
};

const USER_PREFIX = 'user';
const ADMIN_PREFIX = 'admin';

function key(prefix: string, name: string) {
  return `${prefix}:${name}`;
}

function read(storage: Storage, prefix: string): AuthPayload {
  return {
    token: storage.getItem(key(prefix, 'token')) || '',
    userId: storage.getItem(key(prefix, 'userId')) || '',
    username: storage.getItem(key(prefix, 'username')) || '',
    nickname: storage.getItem(key(prefix, 'nickname')) || '',
    role: storage.getItem(key(prefix, 'role')) || ''
  };
}

function write(storage: Storage, prefix: string, payload: AuthPayload) {
  storage.setItem(key(prefix, 'token'), payload.token || '');
  storage.setItem(key(prefix, 'userId'), payload.userId || '');
  storage.setItem(key(prefix, 'username'), payload.username || '');
  storage.setItem(key(prefix, 'nickname'), payload.nickname || '');
  storage.setItem(key(prefix, 'role'), payload.role || '');
}

function clear(storage: Storage, prefix: string) {
  storage.removeItem(key(prefix, 'token'));
  storage.removeItem(key(prefix, 'userId'));
  storage.removeItem(key(prefix, 'username'));
  storage.removeItem(key(prefix, 'nickname'));
  storage.removeItem(key(prefix, 'role'));
}

export function isAdminPath(path?: string) {
  if (path) {
    return path.startsWith('/admin');
  }
  return typeof window !== 'undefined' && window.location.pathname.startsWith('/admin');
}

export function getUserAuth() {
  const payload = read(localStorage, USER_PREFIX);
  return {
    token: payload.token || localStorage.getItem('token') || '',
    userId: payload.userId || localStorage.getItem('userId') || '',
    username: payload.username || localStorage.getItem('username') || '',
    nickname: payload.nickname || localStorage.getItem('nickname') || '',
    role: payload.role || localStorage.getItem('role') || ''
  };
}

export function setUserAuth(payload: AuthPayload) {
  write(localStorage, USER_PREFIX, payload);
  localStorage.setItem('token', payload.token || '');
  localStorage.setItem('userId', payload.userId || '');
  localStorage.setItem('username', payload.username || '');
  localStorage.setItem('nickname', payload.nickname || '');
  localStorage.setItem('role', payload.role || '');
}

export function clearUserAuth() {
  clear(localStorage, USER_PREFIX);
  localStorage.removeItem('token');
  localStorage.removeItem('userId');
  localStorage.removeItem('username');
  localStorage.removeItem('nickname');
  localStorage.removeItem('role');
}

export function getAdminAuth() {
  return read(sessionStorage, ADMIN_PREFIX);
}

export function setAdminAuth(payload: AuthPayload) {
  write(sessionStorage, ADMIN_PREFIX, payload);
}

export function clearAdminAuth() {
  clear(sessionStorage, ADMIN_PREFIX);
}

export function getActiveAuth(path?: string) {
  return isAdminPath(path) ? getAdminAuth() : getUserAuth();
}

export function hasAdminSession() {
  const auth = getAdminAuth();
  return !!auth.token && auth.role === 'ADMIN';
}

<template>
  <template v-if="isAdminLayout">
    <router-view />
  </template>
  <el-container v-else class="app-shell">
    <el-header class="app-header">
      <div class="brand" @click="$router.push('/')">
        <span class="brand-mark"></span>
        <div>
          <div class="brand-title">个人知识分享与交流平台</div>
          <div class="brand-subtitle">Graduation Project Demo</div>
        </div>
      </div>

      <div class="nav-links">
        <el-button text @click="$router.push('/')">首页</el-button>
        <el-button text @click="$router.push('/qa')">问答</el-button>
        <el-button text @click="$router.push('/recommend')">推荐</el-button>
        <el-button text @click="$router.push('/publish')">发布</el-button>
        <el-button text @click="$router.push('/me')">我的</el-button>
      </div>

      <div class="nav-actions">
        <el-badge :value="messageUnreadCount" :hidden="messageUnreadCount === 0">
          <el-button plain @click="$router.push('/messages')">私信</el-button>
        </el-badge>
        <el-button plain @click="$router.push('/notifications')">通知</el-button>
        <template v-if="isLoggedIn">
          <span class="welcome-text">欢迎，{{ displayName }}</span>
          <el-button plain @click="switchAccount">切换账号</el-button>
          <el-button plain @click="logout">退出登录</el-button>
        </template>
        <el-button v-else plain @click="$router.push('/login')">登录</el-button>
      </div>
    </el-header>

    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import api from './api';
import { clearUserAuth, getUserAuth, setUserAuth } from './utils/auth';
import { startRealtime, stopRealtime, type RealtimeEvent } from './utils/realtime';

const router = useRouter();
const route = useRoute();
const token = ref(getUserAuth().token);
const nickname = ref(getUserAuth().nickname);
const username = ref(getUserAuth().username);
const messageUnreadCount = ref(0);
let unreadSyncTimer: ReturnType<typeof window.setInterval> | undefined;

const isLoggedIn = computed(() => !!token.value);
const isAdminLayout = computed(() => route.path.startsWith('/admin'));
const displayName = computed(() => {
  const nicknameValue = (nickname.value || '').trim();
  if (nicknameValue && !/^[?]+$/.test(nicknameValue)) {
    return nicknameValue;
  }
  return (username.value || '').trim() || '用户';
});

async function syncAuthState() {
  const auth = getUserAuth();
  token.value = auth.token;
  nickname.value = auth.nickname;
  username.value = auth.username;

  if (!token.value) {
    stopRealtime();
    messageUnreadCount.value = 0;
    return;
  }

  startRealtime(handleRealtimeEvent);

  try {
    const res = await api.get('/profile/me/space');
    const user = res.data.data?.user;
    if (user?.nickname || user?.username) {
      setUserAuth({
        ...auth,
        nickname: user?.nickname || auth.nickname,
        username: user?.username || auth.username
      });
      token.value = auth.token;
      nickname.value = user?.nickname || auth.nickname;
      username.value = user?.username || auth.username;
    }
  } catch {
    // Ignore profile refresh errors and keep cached auth state.
  }

  await syncUnreadCount();
}

async function syncUnreadCount() {
  const auth = getUserAuth();
  if (!auth.token) {
    messageUnreadCount.value = 0;
    return;
  }

  try {
    const unreadRes = await api.get('/message/unread-count');
    messageUnreadCount.value = Number(unreadRes.data.data?.unreadCount || 0);
  } catch {
    messageUnreadCount.value = 0;
  }
}

function handleRealtimeEvent(event: RealtimeEvent) {
  void syncUnreadCount();
  if (event.type === 'private-message' || event.type === 'notification-read') {
    window.dispatchEvent(new Event('private-message-change'));
  }
  if (event.type === 'notification' || event.type === 'notification-read') {
    window.dispatchEvent(new Event('notification-change'));
  }
}

function clearAuth() {
  clearUserAuth();
  void syncAuthState();
  window.dispatchEvent(new Event('auth-change'));
}

function logout() {
  clearAuth();
  router.push('/login');
}

function switchAccount() {
  clearAuth();
  router.push('/login');
}

onMounted(() => {
  window.addEventListener('auth-change', syncAuthState);
  window.addEventListener('private-message-change', syncUnreadCount);
  window.addEventListener('notification-change', syncUnreadCount);
  window.addEventListener('focus', syncUnreadCount);
  document.addEventListener('visibilitychange', syncUnreadCount);
  unreadSyncTimer = window.setInterval(() => {
    void syncUnreadCount();
  }, 20000);
  void syncAuthState();
});

onBeforeUnmount(() => {
  window.removeEventListener('auth-change', syncAuthState);
  window.removeEventListener('private-message-change', syncUnreadCount);
  window.removeEventListener('notification-change', syncUnreadCount);
  window.removeEventListener('focus', syncUnreadCount);
  document.removeEventListener('visibilitychange', syncUnreadCount);
  if (unreadSyncTimer) {
    clearInterval(unreadSyncTimer);
    unreadSyncTimer = undefined;
  }
  stopRealtime();
});
</script>

<style scoped>
.app-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(44, 123, 229, 0.16), transparent 30%),
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.12), transparent 25%),
    linear-gradient(180deg, #f6fbff 0%, #eef4ff 100%);
}

.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  height: 76px;
  padding: 0 28px;
  border-bottom: 1px solid rgba(15, 23, 42, 0.08);
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(14px);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.brand-mark {
  display: inline-flex;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  background: linear-gradient(135deg, #1677ff 0%, #0f766e 100%);
  box-shadow: 0 10px 24px rgba(22, 119, 255, 0.22);
}

.brand-title {
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.brand-subtitle {
  color: #64748b;
  font-size: 12px;
}

.nav-links {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.16);
}

.nav-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.welcome-text {
  color: #475569;
  font-size: 14px;
}

.app-main {
  padding: 28px;
}

@media (max-width: 1080px) {
  .app-header {
    height: auto;
    padding: 18px;
    flex-wrap: wrap;
  }
}
</style>

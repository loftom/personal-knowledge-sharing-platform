<template>
  <el-container class="app-shell">
    <el-header class="app-header">
      <div class="brand" @click="$router.push('/')">
        <span class="brand-mark">知</span>
        <div>
          <div class="brand-title">知识社区平台</div>
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
        <el-button plain @click="$router.push('/notifications')">通知</el-button>
        <el-button v-if="isAdmin" plain type="primary" @click="$router.push('/admin')">后台管理</el-button>
        <template v-if="isLoggedIn">
          <span class="welcome-text">你好，{{ displayName }}</span>
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
import { useRouter } from 'vue-router';

const router = useRouter();
const token = ref(localStorage.getItem('token') || '');
const nickname = ref(localStorage.getItem('nickname') || '');
const username = ref(localStorage.getItem('username') || '');
const role = ref(localStorage.getItem('role') || '');

const isLoggedIn = computed(() => !!token.value);
const isAdmin = computed(() => role.value === 'ADMIN');
const displayName = computed(() => sanitizeDisplayName(nickname.value, username.value));

function sanitizeDisplayName(rawNickname: string, rawUsername: string) {
  const nicknameValue = (rawNickname || '').trim();
  if (nicknameValue && !/^[?]+$/.test(nicknameValue)) {
    return nicknameValue;
  }
  return (rawUsername || '').trim() || '用户';
}

function syncAuthState() {
  token.value = localStorage.getItem('token') || '';
  nickname.value = localStorage.getItem('nickname') || '';
  username.value = localStorage.getItem('username') || '';
  role.value = localStorage.getItem('role') || '';
}

function clearAuth() {
  localStorage.removeItem('token');
  localStorage.removeItem('userId');
  localStorage.removeItem('nickname');
  localStorage.removeItem('username');
  localStorage.removeItem('role');
  syncAuthState();
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
});

onBeforeUnmount(() => {
  window.removeEventListener('auth-change', syncAuthState);
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
  align-items: center;
  justify-content: center;
  width: 42px;
  height: 42px;
  border-radius: 14px;
  color: #fff;
  font-size: 18px;
  font-weight: 700;
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

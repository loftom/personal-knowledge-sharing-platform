<template>
  <div class="me-page">
    <section class="me-hero">
      <div>
        <div class="section-tag">个人中心</div>
        <h1>我的工作台</h1>
        <p>
          此页面用于集中展示用户的创作内容、数据报告、粉丝关系、成长体系、个人主页与账号管理能力，
          便于形成从内容生产到账号维护的完整个人使用闭环。
        </p>
      </div>
      <div class="hero-meta">
        <div class="meta-card">
          <span>当前用户</span>
          <strong>{{ displayName }}</strong>
          <small>{{ username || '未登录' }}</small>
          <el-button text class="rename-btn" @click="openRename">修改昵称</el-button>
        </div>
      </div>
    </section>

    <el-tabs v-model="activeTab" class="me-tabs">
      <el-tab-pane label="我的内容" name="content">
        <MyContent />
      </el-tab-pane>
      <el-tab-pane label="数据报告" name="report">
        <ReportPage />
      </el-tab-pane>
      <el-tab-pane label="粉丝与关注" name="follow">
        <Followers />
      </el-tab-pane>
      <el-tab-pane label="成长中心" name="growth">
        <GrowthCenter />
      </el-tab-pane>
      <el-tab-pane label="个人主页" name="profile">
        <Profile />
      </el-tab-pane>
      <el-tab-pane label="账号管理" name="account">
        <AccountManage />
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="renameVisible" title="修改昵称" width="420px">
      <el-input v-model="nicknameDraft" maxlength="30" placeholder="请输入新的昵称" />
      <template #footer>
        <el-button @click="renameVisible = false">取消</el-button>
        <el-button type="primary" :loading="renaming" @click="updateNickname">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import api from '../api';
import MyContent from './MyContent.vue';
import ReportPage from './ReportPage.vue';
import Followers from './Followers.vue';
import GrowthCenter from './GrowthCenter.vue';
import Profile from './Profile.vue';
import AccountManage from './AccountManage.vue';

const CUSTOM_SHORTCUTS_KEY = 'customLoginShortcuts';
const activeTab = ref('content');
const nickname = ref(localStorage.getItem('nickname') || '');
const username = ref(localStorage.getItem('username') || '');
const renameVisible = ref(false);
const nicknameDraft = ref('');
const renaming = ref(false);

const displayName = computed(() => {
  const nicknameValue = (nickname.value || '').trim();
  if (nicknameValue && !/^[?]+$/.test(nicknameValue)) {
    return nicknameValue;
  }
  return (username.value || '').trim() || '未登录用户';
});

function syncAuthState() {
  nickname.value = localStorage.getItem('nickname') || '';
  username.value = localStorage.getItem('username') || '';
}

function openRename() {
  nicknameDraft.value = displayName.value;
  renameVisible.value = true;
}

function syncShortcutNickname(nextNickname: string) {
  try {
    const raw = localStorage.getItem(CUSTOM_SHORTCUTS_KEY);
    if (!raw) {
      return;
    }
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return;
    }

    const currentUsername = localStorage.getItem('username') || '';
    const next = parsed.map((item) => {
      if (!item || item.username !== currentUsername) {
        return item;
      }
      return { ...item, nickname: nextNickname };
    });

    localStorage.setItem(CUSTOM_SHORTCUTS_KEY, JSON.stringify(next));
    window.dispatchEvent(new Event('login-shortcuts-change'));
  } catch {
    // Ignore malformed cached shortcut data and keep the profile update successful.
  }
}

async function updateNickname() {
  if (!nicknameDraft.value.trim()) {
    ElMessage.warning('请输入新的昵称');
    return;
  }
  renaming.value = true;
  try {
    const res = await api.put('/profile/me/nickname', { nickname: nicknameDraft.value });
    const nextNickname = res.data.data.nickname || nicknameDraft.value.trim();
    localStorage.setItem('nickname', nextNickname);
    localStorage.setItem('username', res.data.data.username || localStorage.getItem('username') || '');
    syncShortcutNickname(nextNickname);
    window.dispatchEvent(new Event('auth-change'));
    syncAuthState();
    renameVisible.value = false;
    ElMessage.success('昵称修改成功');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '昵称修改失败');
  } finally {
    renaming.value = false;
  }
}

onMounted(() => {
  syncAuthState();
  window.addEventListener('auth-change', syncAuthState);
});

onBeforeUnmount(() => {
  window.removeEventListener('auth-change', syncAuthState);
});
</script>

<style scoped>
.me-page {
  display: grid;
  gap: 18px;
}

.me-hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  padding: 28px;
  border-radius: 30px;
  color: #fff;
  background: linear-gradient(135deg, #0f172a 0%, #1677ff 50%, #0f766e 100%);
}

.me-hero h1 {
  margin: 12px 0 8px;
  font-size: 38px;
}

.me-hero p {
  margin: 0;
  color: rgba(255, 255, 255, 0.85);
  line-height: 1.8;
}

.section-tag {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  font-weight: 700;
}

.hero-meta {
  display: flex;
  align-items: flex-end;
}

.meta-card {
  min-width: 220px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.12);
  backdrop-filter: blur(12px);
}

.meta-card span {
  display: block;
  color: rgba(255, 255, 255, 0.76);
  font-size: 12px;
}

.meta-card strong {
  display: block;
  margin-top: 8px;
  font-size: 24px;
}

.meta-card small {
  display: block;
  margin-top: 4px;
  color: rgba(255, 255, 255, 0.78);
}

.rename-btn {
  margin-top: 10px;
  color: #fff;
  padding-left: 0;
}

.me-tabs :deep(.el-tabs__nav-wrap) {
  padding: 0 6px;
}

@media (max-width: 820px) {
  .me-hero {
    flex-direction: column;
  }
}
</style>

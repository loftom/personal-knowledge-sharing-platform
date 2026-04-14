<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="hero">
        <div class="hero-badge">{{ isAdminMode ? '后台登录入口' : '快捷账号体验' }}</div>
        <h2>{{ isAdminMode ? '系统后台登录' : '登录知识分享平台' }}</h2>
        <p v-if="isAdminMode">
          使用管理员账号进入后台，可查看审核、分类标签、用户画像与平台统计数据。后台登录会使用独立会话，不会覆盖前台普通用户的登录状态。
        </p>
        <p v-else>
          这里提供演示账号与注册入口。你可以直接选择快捷账号体验文章、问答、推荐与个人空间，也可以注册新账号开始完整使用流程。
        </p>

        <div class="hero-actions">
          <el-button
            v-if="!isAdminMode"
            size="large"
            plain
            @click="router.push('/login?mode=admin')"
          >
            进入后台
          </el-button>
          <el-button
            v-else
            size="large"
            plain
            @click="router.push('/login')"
          >
            返回前台登录
          </el-button>
        </div>

        <div class="preset-list">
          <button
            v-for="account in accounts"
            :key="account.username"
            class="preset-card"
            @click="fillAccount(account)"
          >
            <span class="preset-role">{{ account.label }}</span>
            <strong>{{ account.nickname }}</strong>
            <small>{{ account.username }} / {{ account.password }}</small>
          </button>
        </div>
      </div>

      <el-card class="login-card" shadow="never">
        <el-tabs v-model="activeTab" stretch>
          <el-tab-pane label="登录" name="login">
            <el-form @submit.prevent>
              <el-form-item>
                <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large" show-password />
              </el-form-item>
              <div class="account-tip">
                {{
                  isAdminMode
                    ? '管理员登录只允许使用后台账号，登录成功后会进入独立后台，不会影响前台普通用户会话。'
                    : '普通用户可直接使用快捷账号，也可以注册新账号后再登录体验。'
                }}
              </div>
              <el-button class="login-btn" type="primary" size="large" :loading="submitting" @click="login()">
                登录
              </el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane v-if="!isAdminMode" label="注册" name="register">
            <el-form @submit.prevent>
              <el-form-item>
                <el-input v-model="registerForm.username" placeholder="请输入用户名" size="large" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="registerForm.nickname" placeholder="请输入昵称" size="large" />
              </el-form-item>
              <el-form-item>
                <el-input v-model="registerForm.password" type="password" placeholder="请输入密码" size="large" show-password />
              </el-form-item>
              <el-button class="login-btn" type="success" size="large" :loading="submitting" @click="register">
                注册账号
              </el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '../api';
import { clearAdminAuth, setAdminAuth, setUserAuth } from '../utils/auth';

type ShortcutAccount = {
  label: string;
  nickname: string;
  username: string;
  password: string;
};

const CUSTOM_SHORTCUTS_KEY = 'customLoginShortcuts';
const REMOVED_SHORTCUTS_KEY = 'removedLoginShortcutUsernames';

const router = useRouter();
const route = useRoute();
const submitting = ref(false);
const activeTab = ref('login');
const isAdminMode = computed(() => route.query.mode === 'admin');

const loginForm = reactive({ username: '', password: '' });
const registerForm = reactive({ username: '', nickname: '', password: '' });

const presetAccounts: ShortcutAccount[] = [
  { label: '管理员', nickname: '系统管理员', username: 'admin_master', password: 'Admin@123456' },
  { label: '基础作者', nickname: '林知远', username: 'author_lin', password: 'User@123456' },
  { label: '基础作者', nickname: '秦若溪', username: 'author_qin', password: 'User@123456' },
  { label: '基础作者', nickname: '宋明哲', username: 'author_song', password: 'User@123456' },
  { label: '基础读者', nickname: '徐闻笙', username: 'reader_xu', password: 'User@123456' },
  { label: '基础读者', nickname: '何清嘉', username: 'reader_he', password: 'User@123456' },
  { label: '扩展作者', nickname: '陈以衡', username: 'author_chen', password: 'User@123456' },
  { label: '扩展作者', nickname: '罗清越', username: 'author_luo', password: 'User@123456' },
  { label: '扩展作者', nickname: '顾明安', username: 'author_gu', password: 'User@123456' },
  { label: '扩展读者', nickname: '谭书言', username: 'reader_tan', password: 'User@123456' },
  { label: '扩展读者', nickname: '周沐禾', username: 'reader_zhou', password: 'User@123456' },
  { label: '扩展读者', nickname: '吴知夏', username: 'reader_wu', password: 'User@123456' }
];

const canonicalShortcutAccounts: Record<string, Pick<ShortcutAccount, 'label' | 'nickname'>> = {
  admin_master: { label: '管理员', nickname: '系统管理员' },
  author_lin: { label: '基础作者', nickname: '林知远' },
  author_qin: { label: '基础作者', nickname: '秦若溪' },
  author_song: { label: '基础作者', nickname: '宋明哲' },
  reader_xu: { label: '基础读者', nickname: '徐闻笙' },
  reader_he: { label: '基础读者', nickname: '何清嘉' },
  author_chen: { label: '扩展作者', nickname: '陈以衡' },
  author_luo: { label: '扩展作者', nickname: '罗清越' },
  author_gu: { label: '扩展作者', nickname: '顾明安' },
  reader_tan: { label: '扩展读者', nickname: '谭书言' },
  reader_zhou: { label: '扩展读者', nickname: '周沐禾' },
  reader_wu: { label: '扩展读者', nickname: '吴知夏' }
};

const customAccounts = ref<ShortcutAccount[]>([]);
const removedShortcutUsernames = ref<string[]>([]);
const accounts = computed(() => {
  const removed = new Set(removedShortcutUsernames.value);
  const customMap = new Map(customAccounts.value.map((item) => [item.username, item]));
  const mergedPreset = presetAccounts
    .filter((item) => !removed.has(item.username))
    .filter((item) => (isAdminMode.value ? item.username === 'admin_master' : item.username !== 'admin_master'))
    .map((item) => ({ ...item, ...(customMap.get(item.username) || {}) }));
  const extraCustom = customAccounts.value.filter(
    (item) =>
      !removed.has(item.username) &&
      !presetAccounts.some((preset) => preset.username === item.username) &&
      (!isAdminMode.value || item.label === '管理员')
  );
  return [...extraCustom, ...mergedPreset];
});

function loadCustomAccounts(): ShortcutAccount[] {
  try {
    const raw = localStorage.getItem(CUSTOM_SHORTCUTS_KEY);
    if (!raw) {
      return [];
    }
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed
      .filter((item) => item && typeof item.username === 'string' && typeof item.password === 'string')
      .map((item) => ({
        label: typeof item.label === 'string' && item.label.trim() ? item.label : '注册用户',
        nickname: typeof item.nickname === 'string' ? item.nickname : item.username,
        username: item.username,
        password: item.password
      }));
  } catch {
    return [];
  }
}

function normalizeShortcutAccounts(items: ShortcutAccount[]): ShortcutAccount[] {
  return items.map((item) => {
    const canonical = canonicalShortcutAccounts[item.username];
    if (!canonical) {
      return item;
    }
    return {
      ...item,
      label: canonical.label,
      nickname: canonical.nickname
    };
  });
}

function loadRemovedShortcutUsernames(): string[] {
  try {
    const raw = localStorage.getItem(REMOVED_SHORTCUTS_KEY);
    if (!raw) {
      return [];
    }
    const parsed = JSON.parse(raw);
    if (!Array.isArray(parsed)) {
      return [];
    }
    return parsed.filter((item) => typeof item === 'string' && item.trim());
  } catch {
    return [];
  }
}

function persistCustomAccounts() {
  localStorage.setItem(CUSTOM_SHORTCUTS_KEY, JSON.stringify(customAccounts.value));
}

function persistRemovedShortcutUsernames() {
  localStorage.setItem(REMOVED_SHORTCUTS_KEY, JSON.stringify(removedShortcutUsernames.value));
}

function refreshCustomAccounts() {
  const nextCustomAccounts = normalizeShortcutAccounts(loadCustomAccounts());
  customAccounts.value = nextCustomAccounts;
  persistCustomAccounts();
  removedShortcutUsernames.value = loadRemovedShortcutUsernames();
}

function clearRemovedShortcutUsername(username: string) {
  if (!username.trim()) {
    return;
  }
  const next = removedShortcutUsernames.value.filter((item) => item !== username);
  if (next.length === removedShortcutUsernames.value.length) {
    return;
  }
  removedShortcutUsernames.value = next;
  persistRemovedShortcutUsernames();
}

function upsertShortcutAccount(account: ShortcutAccount) {
  clearRemovedShortcutUsername(account.username);
  const canonical = canonicalShortcutAccounts[account.username];
  const nextAccount = canonical ? { ...account, ...canonical } : account;
  const next = [
    nextAccount,
    ...customAccounts.value.filter((item) => item.username !== account.username)
  ];
  customAccounts.value = next.slice(0, 8);
  persistCustomAccounts();
}

function fillAccount(account: { username: string; password: string }) {
  activeTab.value = 'login';
  loginForm.username = account.username;
  loginForm.password = account.password;
}

async function login(account?: { username: string; password: string }) {
  const payload = account || loginForm;
  submitting.value = true;
  try {
    const res = await api.post('/auth/login', payload);
    const nextUsername = res.data.data.username || payload.username;
    const nextNickname = res.data.data.nickname || nextUsername;
    const nextRole = res.data.data.role || 'USER';

    if (isAdminMode.value) {
      if (nextRole !== 'ADMIN') {
        clearAdminAuth();
        ElMessage.error('后台登录仅允许管理员账号');
        return;
      }
      setAdminAuth({
        token: res.data.data.token,
        userId: String(res.data.data.userId),
        nickname: nextNickname,
        username: nextUsername,
        role: nextRole
      });
      upsertShortcutAccount({
        label: '管理员',
        nickname: nextNickname,
        username: nextUsername,
        password: payload.password
      });
      window.dispatchEvent(new Event('admin-auth-change'));
      window.dispatchEvent(new Event('login-shortcuts-change'));
      ElMessage.success('后台登录成功');
      router.push('/admin');
      return;
    }

    setUserAuth({
      token: res.data.data.token,
      userId: String(res.data.data.userId),
      nickname: nextNickname,
      username: nextUsername,
      role: nextRole
    });
    upsertShortcutAccount({
      label:
        customAccounts.value.find((item) => item.username === nextUsername)?.label ||
        presetAccounts.find((item) => item.username === nextUsername)?.label ||
        '注册用户',
      nickname: nextNickname,
      username: nextUsername,
      password: payload.password
    });
    window.dispatchEvent(new Event('auth-change'));
    window.dispatchEvent(new Event('login-shortcuts-change'));
    ElMessage.success('登录成功');
    router.push(String(route.query.redirect || '/'));
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '登录失败');
  } finally {
    submitting.value = false;
  }
}

async function register() {
  if (!registerForm.username.trim() || !registerForm.nickname.trim() || !registerForm.password.trim()) {
    ElMessage.warning('请输入完整的用户名、昵称和密码');
    return;
  }
  submitting.value = true;
  try {
    await api.post('/auth/register', registerForm);
    upsertShortcutAccount({
      label: '注册用户',
      nickname: registerForm.nickname.trim(),
      username: registerForm.username.trim(),
      password: registerForm.password
    });
    ElMessage.success('注册成功，请登录');
    loginForm.username = registerForm.username;
    loginForm.password = registerForm.password;
    registerForm.username = '';
    registerForm.nickname = '';
    registerForm.password = '';
    activeTab.value = 'login';
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '注册失败');
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  refreshCustomAccounts();
  if (isAdminMode.value) {
    activeTab.value = 'login';
  }
  if (route.query.reason === 'login-required') {
    ElMessage.warning('请先登录后再访问该页面');
  }
  window.addEventListener('login-shortcuts-change', refreshCustomAccounts);
  window.addEventListener('storage', refreshCustomAccounts);
});

onBeforeUnmount(() => {
  window.removeEventListener('login-shortcuts-change', refreshCustomAccounts);
  window.removeEventListener('storage', refreshCustomAccounts);
});
</script>

<style scoped>
.login-page {
  min-height: calc(100vh - 132px);
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-panel {
  width: min(980px, 100%);
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 24px;
  align-items: center;
}

.hero {
  padding: 28px;
}

.hero-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  color: #0f766e;
  background: rgba(16, 185, 129, 0.12);
  font-size: 12px;
  font-weight: 700;
}

.hero h2 {
  margin: 18px 0 12px;
  color: #0f172a;
  font-size: 40px;
  line-height: 1.1;
}

.hero p {
  margin: 0;
  color: #475569;
  line-height: 1.8;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 18px;
}

.preset-list {
  display: grid;
  gap: 12px;
  margin-top: 24px;
  max-height: 520px;
  overflow: auto;
  padding-right: 6px;
}

.preset-card {
  display: grid;
  gap: 4px;
  text-align: left;
  padding: 16px 18px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
}

.preset-role {
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.preset-card strong {
  color: #0f172a;
  font-size: 18px;
}

.preset-card small {
  color: #64748b;
}

.login-card {
  border: none;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12);
}

.account-tip {
  margin-bottom: 12px;
  color: #64748b;
  font-size: 13px;
}

.login-btn {
  width: 100%;
}

@media (max-width: 900px) {
  .login-panel {
    grid-template-columns: 1fr;
  }

  .hero {
    padding: 12px 0 0;
  }
}
</style>

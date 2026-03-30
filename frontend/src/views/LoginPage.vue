<template>
  <div class="login-page">
    <div class="login-panel">
      <div class="hero">
        <div class="hero-badge">账号入口</div>
        <h2>登录知识社区</h2>
        <p>
          登录后可进入内容创作、个人工作台与后台审核流程。左侧提供当前演示环境中的快捷切换账号，
          便于在普通用户与管理员之间快速完成测试和演示。
        </p>

        <div class="preset-list">
          <button v-for="account in accounts" :key="account.username" class="preset-card" @click="fillAccount(account)">
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
              <div class="account-tip">点击左侧账号卡片后，可直接使用当前表单完成登录。</div>
              <el-button class="login-btn" type="primary" size="large" :loading="submitting" @click="login()">登录</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
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
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '../api';

const router = useRouter();
const submitting = ref(false);
const activeTab = ref('login');

const loginForm = reactive({ username: '', password: '' });
const registerForm = reactive({ username: '', nickname: '', password: '' });

const accounts = [
  { label: '管理员', nickname: '系统管理员', username: 'admin_master', password: 'Admin@123456' },
  { label: '作者账号', nickname: '林知远', username: 'author_lin', password: 'User@123456' },
  { label: '作者账号', nickname: '秦若溪', username: 'author_qin', password: 'User@123456' },
  { label: '作者账号', nickname: '宋明哲', username: 'author_song', password: 'User@123456' },
  { label: '普通用户', nickname: '徐闻笙', username: 'reader_xu', password: 'User@123456' },
  { label: '普通用户', nickname: '何清嘉', username: 'reader_he', password: 'User@123456' },
  { label: '新增作者', nickname: '陈以衡', username: 'author_chen', password: 'User@123456' },
  { label: '新增作者', nickname: '罗清越', username: 'author_luo', password: 'User@123456' },
  { label: '新增作者', nickname: '顾明安', username: 'author_gu', password: 'User@123456' },
  { label: '新增读者', nickname: '谭书言', username: 'reader_tan', password: 'User@123456' },
  { label: '新增读者', nickname: '周沐禾', username: 'reader_zhou', password: 'User@123456' },
  { label: '新增读者', nickname: '吴知夏', username: 'reader_wu', password: 'User@123456' }
];

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
    localStorage.setItem('token', res.data.data.token);
    localStorage.setItem('userId', String(res.data.data.userId));
    localStorage.setItem('nickname', res.data.data.nickname || '');
    localStorage.setItem('username', res.data.data.username || payload.username);
    localStorage.setItem('role', res.data.data.role || 'USER');
    window.dispatchEvent(new Event('auth-change'));
    ElMessage.success('登录成功');
    router.push(res.data.data.role === 'ADMIN' ? '/admin' : '/');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '登录失败');
  } finally {
    submitting.value = false;
  }
}

async function register() {
  if (!registerForm.username.trim() || !registerForm.nickname.trim() || !registerForm.password.trim()) {
    ElMessage.warning('请完整填写用户名、昵称和密码');
    return;
  }
  submitting.value = true;
  try {
    await api.post('/auth/register', registerForm);
    ElMessage.success('注册成功，请直接登录');
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

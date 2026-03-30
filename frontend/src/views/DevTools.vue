<template>
  <div style="max-width:600px;margin:40px auto;">
    <h2>开发辅助工具（仅本地）</h2>
    <el-input v-model="token" placeholder="填写测试 JWT token" clearable />
      <div style="margin-top:12px">
        <el-input v-model="username" placeholder="测试用户名（示例: testuser）" />
        <el-input v-model="password" placeholder="测试密码（示例: password）" type="password" />
        <el-button type="success" style="margin-top:8px" @click="autoLogin">注册并登录（测试）</el-button>
      </div>
    <div style="margin-top:8px">
      <el-button type="primary" @click="save">保存到 localStorage</el-button>
      <el-button @click="copyToken" :disabled="!token">复制 token</el-button>
      <el-button @click="clear">清空 token</el-button>
    </div>
    <div style="margin-top:12px;color:#666">提示：保存后刷新页面，前端会在请求头中带上该 token，便于联调需要授权的接口。</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import api from '../api';

const token = ref(localStorage.getItem('token') || '');
const username = ref('testuser');
const password = ref('password');

function copyToken() {
  if (!token.value) return window.alert('token 为空');
  navigator.clipboard.writeText(token.value).then(() => window.alert('token 已复制到剪贴板'))
    .catch(() => window.alert('复制失败，请手动复制'));
}

function save() {
  localStorage.setItem('token', token.value);
  window.alert('token 已保存到 localStorage');
}

function clear() {
  localStorage.removeItem('token');
  token.value = '';
  window.alert('token 已清空');
}

async function autoLogin() {
  try {
    // 先尝试注册（若已存在可能返回错误），再登录获取 token
    await api.post('/auth/register', { username: username.value, password: password.value });
  } catch (e) {
    // ignore register error
  }
  const res = await api.post('/auth/login', { username: username.value, password: password.value });
  const t = res.data?.data?.token || res.data?.token || '';
  if (t) {
    localStorage.setItem('token', t);
    token.value = t;
    window.alert('登录成功，token 已保存');
  } else {
    window.alert('未收到 token，登录可能失败');
  }
}
</script>

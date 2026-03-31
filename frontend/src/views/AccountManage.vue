<template>
  <div class="account-manage-page">
    <div class="page-head">
      <div>
        <div class="section-tag">账号管理</div>
        <h2>密码与账号维护</h2>
        <p>此处提供当前登录账号的密码重置与账号注销功能，便于在本地演示环境中快速完成测试切换。</p>
      </div>
    </div>

    <div class="card-grid">
      <el-card class="panel-card" shadow="never">
        <template #header>
          <div class="card-title">密码重置</div>
        </template>
        <div class="form-wrap">
          <el-input v-model="newPassword" type="password" show-password placeholder="请输入新的登录密码" />
          <el-input v-model="confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
          <el-button type="primary" :loading="resetting" @click="resetPassword">重置密码</el-button>
        </div>
      </el-card>

      <el-card class="panel-card danger-card" shadow="never">
        <template #header>
          <div class="card-title">账号删除</div>
        </template>
        <div class="danger-wrap">
          <p>注销后将无法继续登录，当前账号发布的内容会被下架，评论和回答会被隐藏，仅建议用于本地测试。</p>
          <el-button type="danger" @click="deleteVisible = true">删除当前账号</el-button>
        </div>
      </el-card>
    </div>

    <el-dialog v-model="deleteVisible" title="确认删除账号" width="460px">
      <p class="delete-tip">此操作不可撤销。删除后会退出当前登录状态，并清理该账号在演示环境中的主要互动能力。</p>
      <el-input v-model="deleteConfirmText" :placeholder="`请输入 ${username} 以确认删除`" />
      <template #footer>
        <el-button @click="deleteVisible = false">取消</el-button>
        <el-button type="danger" :loading="deleting" @click="deleteAccount">确认删除</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '../api';

const router = useRouter();
const username = computed(() => localStorage.getItem('username') || '当前账号');
const newPassword = ref('');
const confirmPassword = ref('');
const resetting = ref(false);
const deleteVisible = ref(false);
const deleteConfirmText = ref('');
const deleting = ref(false);

function clearAuthState() {
  localStorage.removeItem('token');
  localStorage.removeItem('userId');
  localStorage.removeItem('nickname');
  localStorage.removeItem('username');
  localStorage.removeItem('role');
  window.dispatchEvent(new Event('auth-change'));
}

async function resetPassword() {
  if (!newPassword.value.trim()) {
    ElMessage.warning('请输入新密码');
    return;
  }
  if (newPassword.value.trim().length < 6) {
    ElMessage.warning('新密码长度不能少于 6 位');
    return;
  }
  if (newPassword.value !== confirmPassword.value) {
    ElMessage.warning('两次输入的新密码不一致');
    return;
  }

  resetting.value = true;
  try {
    await api.put('/profile/me/password', { newPassword: newPassword.value.trim() });
    newPassword.value = '';
    confirmPassword.value = '';
    ElMessage.success('密码重置成功，请使用新密码重新登录测试');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '密码重置失败');
  } finally {
    resetting.value = false;
  }
}

async function deleteAccount() {
  if (deleteConfirmText.value.trim() !== username.value) {
    ElMessage.warning(`请输入 ${username.value} 以确认删除`);
    return;
  }

  deleting.value = true;
  try {
    await api.delete('/profile/me');
    clearAuthState();
    deleteVisible.value = false;
    ElMessage.success('账号已删除，已退出登录');
    router.push('/login');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '账号删除失败');
  } finally {
    deleting.value = false;
  }
}
</script>

<style scoped>
.account-manage-page {
  display: grid;
  gap: 18px;
}

.page-head h2 {
  margin: 8px 0 6px;
  font-size: 30px;
}

.page-head p {
  margin: 0;
  color: #64748b;
  line-height: 1.75;
}

.section-tag {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  color: #0f766e;
  background: rgba(16, 185, 129, 0.12);
  font-size: 12px;
  font-weight: 700;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.panel-card {
  border: none;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 50px rgba(15, 23, 42, 0.08);
}

.card-title {
  font-weight: 700;
  color: #0f172a;
}

.form-wrap,
.danger-wrap {
  display: grid;
  gap: 14px;
}

.danger-wrap p,
.delete-tip {
  margin: 0;
  color: #64748b;
  line-height: 1.75;
}

.danger-card {
  border: 1px solid rgba(239, 68, 68, 0.16);
}

@media (max-width: 820px) {
  .card-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="admin-manage-page">
    <div class="page-head">
      <div>
        <div class="section-tag">平台管理</div>
        <h2>用户与内容管理</h2>
        <p>支持用户启停用、内容状态统一管理，便于答辩演示后台治理能力闭环。</p>
      </div>
      <el-button @click="reloadAll">刷新全部</el-button>
    </div>

    <el-tabs v-model="activeTab" class="panel-tabs">
      <el-tab-pane label="用户管理" name="users">
        <el-card class="panel-card" shadow="never">
          <div class="toolbar-row">
            <el-input v-model="userQuery.keyword" clearable placeholder="搜索用户名或昵称" style="max-width: 320px" @keyup.enter="loadUsers" />
            <el-select v-model="userQuery.status" style="width: 180px" @change="loadUsers">
              <el-option label="全部状态" :value="-1" />
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
            <el-button type="primary" plain @click="loadUsers">查询</el-button>
          </div>

          <el-table :data="users" :loading="loadingUsers">
            <el-table-column prop="username" label="用户名" min-width="160" />
            <el-table-column prop="nickname" label="昵称" min-width="140" />
            <el-table-column label="角色" width="120">
              <template #default="{ row }">
                <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="Number(row.status) === 1 ? 'success' : 'warning'">{{ Number(row.status) === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="内容量" width="130">
              <template #default="{ row }">{{ row.contentCount || 0 }}</template>
            </el-table-column>
            <el-table-column label="已发布" width="130">
              <template #default="{ row }">{{ row.publishedContentCount || 0 }}</template>
            </el-table-column>
            <el-table-column prop="createdAt" label="注册时间" min-width="180" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button
                  v-if="row.role !== 'ADMIN' && Number(row.status) === 1"
                  size="small"
                  type="warning"
                  plain
                  @click="changeUserStatus(row, 0)"
                >停用</el-button>
                <el-button
                  v-if="row.role !== 'ADMIN' && Number(row.status) === 0"
                  size="small"
                  type="primary"
                  plain
                  @click="changeUserStatus(row, 1)"
                >启用</el-button>
                <span v-if="row.role === 'ADMIN'" class="muted-text">管理员不可停用</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="内容管理" name="contents">
        <el-card class="panel-card" shadow="never">
          <div class="toolbar-row wrap">
            <el-input v-model="contentQuery.keyword" clearable placeholder="搜索标题或摘要" style="min-width: 240px; max-width: 320px" @keyup.enter="loadContents" />
            <el-select v-model="contentQuery.status" style="width: 170px" @change="loadContents">
              <el-option label="全部状态" value="ALL" />
              <el-option label="待审核" value="PENDING_REVIEW" />
              <el-option label="已发布" value="PUBLISHED" />
              <el-option label="已驳回" value="REJECTED" />
              <el-option label="已下架" value="OFFLINE" />
            </el-select>
            <el-select v-model="contentQuery.type" style="width: 150px" @change="loadContents">
              <el-option label="全部类型" value="ALL" />
              <el-option label="文章" value="ARTICLE" />
              <el-option label="教程" value="TUTORIAL" />
              <el-option label="问答" value="QUESTION" />
            </el-select>
            <el-button type="primary" plain @click="loadContents">查询</el-button>
          </div>

          <el-table :data="contents" :loading="loadingContents">
            <el-table-column prop="title" label="标题" min-width="240" />
            <el-table-column prop="authorName" label="作者" min-width="120" />
            <el-table-column prop="type" label="类型" width="110" />
            <el-table-column label="状态" width="130">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="互动" min-width="170">
              <template #default="{ row }">阅 {{ row.viewCount || 0 }} · 赞 {{ row.likeCount || 0 }} · 藏 {{ row.favoriteCount || 0 }}</template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" min-width="170" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-select
                  v-model="row._nextStatus"
                  size="small"
                  style="width: 120px"
                  placeholder="状态"
                >
                  <el-option label="待审核" value="PENDING_REVIEW" />
                  <el-option label="已发布" value="PUBLISHED" />
                  <el-option label="已驳回" value="REJECTED" />
                  <el-option label="已下架" value="OFFLINE" />
                </el-select>
                <el-button size="small" type="primary" plain @click="updateContentStatus(row)">应用</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '../api';

const activeTab = ref<'users' | 'contents'>('users');
const users = ref<any[]>([]);
const contents = ref<any[]>([]);
const loadingUsers = ref(false);
const loadingContents = ref(false);

const userQuery = reactive({
  keyword: '',
  status: -1
});

const contentQuery = reactive({
  keyword: '',
  status: 'ALL',
  type: 'ALL'
});

function statusLabel(status?: string) {
  if (status === 'PUBLISHED') return '已发布';
  if (status === 'REJECTED') return '已驳回';
  if (status === 'OFFLINE') return '已下架';
  return '待审核';
}

function statusType(status?: string) {
  if (status === 'PUBLISHED') return 'success';
  if (status === 'REJECTED') return 'warning';
  if (status === 'OFFLINE') return 'danger';
  return 'info';
}

async function loadUsers() {
  loadingUsers.value = true;
  try {
    const res = await api.get('/admin/manage/users', {
      params: {
        keyword: userQuery.keyword || undefined,
        status: userQuery.status === -1 ? undefined : userQuery.status
      }
    });
    users.value = res.data.data || [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载用户列表失败');
  } finally {
    loadingUsers.value = false;
  }
}

async function changeUserStatus(row: any, status: 0 | 1) {
  try {
    await api.post(`/admin/manage/users/${row.id}/status`, { status });
    ElMessage.success(status === 1 ? '用户已启用' : '用户已停用');
    await loadUsers();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '更新用户状态失败');
  }
}

async function loadContents() {
  loadingContents.value = true;
  try {
    const res = await api.get('/admin/manage/contents', {
      params: {
        keyword: contentQuery.keyword || undefined,
        status: contentQuery.status === 'ALL' ? undefined : contentQuery.status,
        type: contentQuery.type === 'ALL' ? undefined : contentQuery.type
      }
    });
    contents.value = (res.data.data || []).map((item: any) => ({
      ...item,
      _nextStatus: item.status
    }));
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载内容列表失败');
  } finally {
    loadingContents.value = false;
  }
}

async function updateContentStatus(row: any) {
  if (!row._nextStatus || row._nextStatus === row.status) {
    ElMessage.info('请选择新的内容状态');
    return;
  }
  try {
    const reason = await ElMessageBox.prompt('请输入操作说明（可选）', '内容状态变更', {
      inputPlaceholder: '例如：违规内容下架、二次复审通过',
      confirmButtonText: '确认变更',
      cancelButtonText: '取消',
      showCancelButton: true
    }).then((res) => res.value).catch(() => '');

    await api.post(`/admin/manage/contents/${row.id}/status`, {
      status: row._nextStatus,
      reason: reason || undefined
    });
    ElMessage.success('内容状态已更新');
    await loadContents();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '更新内容状态失败');
  }
}

async function reloadAll() {
  await Promise.all([loadUsers(), loadContents()]);
}

watch(activeTab, async (tab) => {
  if (tab === 'users' && users.value.length === 0) {
    await loadUsers();
  }
  if (tab === 'contents' && contents.value.length === 0) {
    await loadContents();
  }
});

onMounted(async () => {
  await loadUsers();
});
</script>

<style scoped>
.admin-manage-page { display: grid; gap: 18px; }
.page-head { display: flex; justify-content: space-between; align-items: end; gap: 16px; }
.page-head h2 { margin: 8px 0 6px; font-size: 30px; }
.page-head p { margin: 0; color: #64748b; line-height: 1.75; }
.section-tag { display: inline-block; padding: 6px 12px; border-radius: 999px; color: #0f766e; background: rgba(16, 185, 129, 0.12); font-size: 12px; font-weight: 700; }
.panel-tabs :deep(.el-tabs__header) { margin: 0; }
.panel-card { border: none; border-radius: 24px; background: rgba(255,255,255,.92); box-shadow: 0 18px 45px rgba(15,23,42,.08); }
.toolbar-row { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.toolbar-row.wrap { flex-wrap: wrap; }
.muted-text { color: #94a3b8; font-size: 12px; }
@media (max-width: 860px) {
  .toolbar-row { flex-wrap: wrap; }
}
</style>

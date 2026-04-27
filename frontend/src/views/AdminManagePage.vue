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

    <div class="overview-grid">
      <div class="overview-card">
        <span>用户总量</span>
        <strong>{{ userSummary.total }}</strong>
        <small>启用 {{ userSummary.active }} / 停用 {{ userSummary.disabled }}</small>
      </div>
      <div class="overview-card">
        <span>内容总量</span>
        <strong>{{ contentSummary.total }}</strong>
        <small>已发布 {{ contentSummary.published }} / 待审核 {{ contentSummary.pending }}</small>
      </div>
      <div class="overview-card large">
        <span>内容状态占比图</span>
        <div class="status-bars">
          <div v-for="item in statusBars" :key="item.label" class="status-row">
            <label>{{ item.label }}</label>
            <div class="bar-track">
              <i class="bar-fill" :style="{ width: `${item.percent}%`, background: item.color }"></i>
            </div>
            <strong>{{ item.value }}</strong>
          </div>
        </div>
      </div>
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
            <el-button plain @click="exportUsersCsv">导出 CSV</el-button>
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
            <el-select v-model="batchStatus" style="width: 180px">
              <el-option label="批量状态变更" value="" />
              <el-option label="批量改为待审核" value="PENDING_REVIEW" />
              <el-option label="批量改为已发布" value="PUBLISHED" />
              <el-option label="批量改为已驳回" value="REJECTED" />
              <el-option label="批量改为已下架" value="OFFLINE" />
            </el-select>
            <el-button type="primary" plain @click="loadContents">查询</el-button>
            <el-button type="warning" plain @click="batchUpdateContents">批量应用</el-button>
            <el-button plain @click="exportContentsCsv">导出 CSV</el-button>
          </div>

          <el-table :data="contents" :loading="loadingContents" @selection-change="onContentSelectionChange">
            <el-table-column type="selection" width="42" />
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
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import api from '../api';

const activeTab = ref<'users' | 'contents'>('users');
const users = ref<any[]>([]);
const contents = ref<any[]>([]);
const loadingUsers = ref(false);
const loadingContents = ref(false);
const selectedContents = ref<any[]>([]);
const batchStatus = ref('');

const userSummary = computed(() => ({
  total: users.value.length,
  active: users.value.filter((item) => Number(item.status) === 1).length,
  disabled: users.value.filter((item) => Number(item.status) !== 1).length
}));

const contentSummary = computed(() => ({
  total: contents.value.length,
  pending: contents.value.filter((item) => item.status === 'PENDING_REVIEW').length,
  published: contents.value.filter((item) => item.status === 'PUBLISHED').length,
  rejected: contents.value.filter((item) => item.status === 'REJECTED').length,
  offline: contents.value.filter((item) => item.status === 'OFFLINE').length
}));

const statusBars = computed(() => {
  const rows = [
    { label: '待审核', value: contentSummary.value.pending, color: 'linear-gradient(90deg,#f59e0b,#fb7185)' },
    { label: '已发布', value: contentSummary.value.published, color: 'linear-gradient(90deg,#10b981,#22c55e)' },
    { label: '已驳回', value: contentSummary.value.rejected, color: 'linear-gradient(90deg,#8b5cf6,#6366f1)' },
    { label: '已下架', value: contentSummary.value.offline, color: 'linear-gradient(90deg,#ef4444,#dc2626)' }
  ];
  const max = Math.max(...rows.map((item) => item.value), 1);
  return rows.map((item) => ({
    ...item,
    percent: Math.max(Math.round((item.value / max) * 100), item.value > 0 ? 10 : 0)
  }));
});

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

function onContentSelectionChange(rows: any[]) {
  selectedContents.value = rows;
}

async function batchUpdateContents() {
  if (!batchStatus.value) {
    ElMessage.info('请选择批量目标状态');
    return;
  }
  if (!selectedContents.value.length) {
    ElMessage.warning('请先勾选要批量处理的内容');
    return;
  }
  try {
    await Promise.all(selectedContents.value.map((row) => api.post(`/admin/manage/contents/${row.id}/status`, {
      status: batchStatus.value,
      reason: `批量更新为 ${batchStatus.value}`
    })));
    ElMessage.success(`已批量更新 ${selectedContents.value.length} 条内容`);
    selectedContents.value = [];
    await loadContents();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '批量更新失败');
  }
}

function downloadCsv(filename: string, header: string[], rows: Array<Array<string | number>>) {
  const escapeCell = (value: string | number) => `"${String(value ?? '').replace(/"/g, '""')}"`;
  const content = [header.map(escapeCell).join(','), ...rows.map((row) => row.map(escapeCell).join(','))].join('\n');
  const blob = new Blob([`\uFEFF${content}`], { type: 'text/csv;charset=utf-8;' });
  const link = document.createElement('a');
  link.href = URL.createObjectURL(blob);
  link.download = filename;
  link.click();
  URL.revokeObjectURL(link.href);
}

function exportUsersCsv() {
  downloadCsv(
    `users-${Date.now()}.csv`,
    ['ID', '用户名', '昵称', '角色', '状态', '内容量', '已发布量', '注册时间'],
    users.value.map((user) => [
      user.id,
      user.username || '',
      user.nickname || '',
      user.role || '',
      Number(user.status) === 1 ? '启用' : '停用',
      user.contentCount || 0,
      user.publishedContentCount || 0,
      user.createdAt || ''
    ])
  );
}

function exportContentsCsv() {
  downloadCsv(
    `contents-${Date.now()}.csv`,
    ['ID', '标题', '类型', '状态', '可见性', '作者', '阅读', '点赞', '收藏', '创建时间'],
    contents.value.map((content) => [
      content.id,
      content.title || '',
      content.type || '',
      statusLabel(content.status),
      content.visibility || '',
      content.authorName || '',
      content.viewCount || 0,
      content.likeCount || 0,
      content.favoriteCount || 0,
      content.createdAt || ''
    ])
  );
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
.overview-grid { display:grid; grid-template-columns:repeat(3,minmax(0,1fr)); gap:12px; }
.overview-card { padding:16px; border-radius:20px; background:linear-gradient(135deg,#ffffff,#f8fbff); box-shadow:0 10px 25px rgba(15,23,42,.07); }
.overview-card.large { grid-column: span 1; }
.overview-card span { color:#64748b; font-size:12px; }
.overview-card strong { display:block; margin-top:8px; color:#0f172a; font-size:30px; }
.overview-card small { display:block; margin-top:6px; color:#64748b; }
.status-bars { display:grid; gap:8px; margin-top:8px; }
.status-row { display:grid; grid-template-columns:70px 1fr 36px; align-items:center; gap:8px; }
.status-row label { color:#64748b; font-size:12px; }
.bar-track { height:8px; border-radius:999px; background:#e2e8f0; overflow:hidden; }
.bar-fill { display:block; height:100%; border-radius:inherit; }
.status-row strong { color:#0f172a; font-size:12px; text-align:right; }
.section-tag { display: inline-block; padding: 6px 12px; border-radius: 999px; color: #0f766e; background: rgba(16, 185, 129, 0.12); font-size: 12px; font-weight: 700; }
.panel-tabs :deep(.el-tabs__header) { margin: 0; }
.panel-card { border: none; border-radius: 24px; background: rgba(255,255,255,.92); box-shadow: 0 18px 45px rgba(15,23,42,.08); }
.toolbar-row { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.toolbar-row.wrap { flex-wrap: wrap; }
.muted-text { color: #94a3b8; font-size: 12px; }
@media (max-width: 860px) {
  .overview-grid { grid-template-columns:1fr; }
  .toolbar-row { flex-wrap: wrap; }
}
</style>

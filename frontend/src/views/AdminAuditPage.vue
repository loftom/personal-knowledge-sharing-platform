<template>
  <div class="admin-audit-page">
    <div class="page-head">
      <div>
        <div class="section-tag">内容审核</div>
        <h2>审核工作台</h2>
        <p>管理员可在此集中处理待审核内容，并查看已发布、已驳回与已下架记录。驳回仅用于待审核内容，下架仅用于已发布内容。</p>
      </div>
      <el-button @click="load">刷新列表</el-button>
    </div>

    <div class="summary-grid">
      <div class="summary-card">
        <span>待审核内容</span>
        <strong>{{ overview.pendingCount || 0 }}</strong>
      </div>
      <div class="summary-card">
        <span>已发布内容</span>
        <strong>{{ overview.publishedCount || 0 }}</strong>
      </div>
      <div class="summary-card">
        <span>已下架内容</span>
        <strong>{{ overview.offlineCount || 0 }}</strong>
      </div>
      <div class="summary-card">
        <span>文章待审</span>
        <strong>{{ overview.articlePendingCount || 0 }}</strong>
      </div>
      <div class="summary-card">
        <span>问答待审</span>
        <strong>{{ overview.questionPendingCount || 0 }}</strong>
      </div>
    </div>

    <el-card class="panel-card" shadow="never">
      <div class="toolbar">
        <el-select v-model="status" placeholder="审核状态" @change="loadList">
          <el-option label="待审核" value="PENDING_REVIEW" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已下架" value="OFFLINE" />
        </el-select>
      </div>

      <el-table :data="list" :loading="loading">
        <el-table-column prop="title" label="标题" min-width="240" />
        <el-table-column prop="type" label="类型" width="110" />
        <el-table-column prop="authorName" label="作者" width="140" />
        <el-table-column prop="status" label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" min-width="180" />
        <el-table-column label="操作" width="360">
          <template #default="{ row }">
            <el-button size="small" @click="openPreview(row)">预览</el-button>
            <el-button v-if="row.status === 'PENDING_REVIEW'" size="small" type="primary" @click="approve(row.id)">通过</el-button>
            <el-button v-if="row.status === 'PENDING_REVIEW'" size="small" type="warning" plain @click="openReject(row)">驳回</el-button>
            <el-button v-if="row.status === 'PUBLISHED'" size="small" type="danger" plain @click="openOffline(row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && list.length === 0" class="empty-wrap">
        <el-empty description="当前状态下暂无内容记录。" />
      </div>
    </el-card>

    <el-dialog v-model="previewVisible" title="内容预览" width="760px">
      <template v-if="previewItem">
        <h3>{{ previewItem.title }}</h3>
        <p class="preview-meta">
          类型：{{ previewItem.type }} · 作者：{{ previewItem.authorName || `用户 ${previewItem.authorId}` }} · 状态：{{ statusLabel(previewItem.status) }}
        </p>
        <p v-if="previewItem.summary" class="preview-summary">{{ previewItem.summary }}</p>
        <div class="preview-body" v-html="previewItem.body || '暂无正文内容'"></div>
      </template>
    </el-dialog>

    <el-dialog v-model="rejectVisible" title="驳回原因" width="520px">
      <el-input v-model="rejectReason" type="textarea" :rows="5" placeholder="请输入驳回原因。驳回适用于待审核内容未通过审核的场景。" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="warning" @click="rejectConfirm">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="offlineVisible" title="下架原因" width="520px">
      <el-input v-model="offlineReason" type="textarea" :rows="5" placeholder="请输入下架原因。下架适用于已发布内容被撤回的场景。" />
      <template #footer>
        <el-button @click="offlineVisible = false">取消</el-button>
        <el-button type="danger" @click="offlineConfirm">确认下架</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import api from '../api';

const list = ref<any[]>([]);
const loading = ref(false);
const status = ref('PENDING_REVIEW');
const overview = ref<any>({});
const previewVisible = ref(false);
const previewItem = ref<any>(null);
const rejectVisible = ref(false);
const rejectReason = ref('');
const offlineVisible = ref(false);
const offlineReason = ref('');
const currentId = ref<number | null>(null);

function statusLabel(value?: string) {
  if (value === 'PUBLISHED') return '已发布';
  if (value === 'REJECTED') return '已驳回';
  if (value === 'OFFLINE') return '已下架';
  return '待审核';
}

function statusTagType(value?: string) {
  if (value === 'PUBLISHED') return 'success';
  if (value === 'REJECTED') return 'warning';
  if (value === 'OFFLINE') return 'danger';
  return 'info';
}

async function loadOverview() {
  const res = await api.get('/admin/audit/overview');
  overview.value = res.data.data || {};
}

async function loadList() {
  loading.value = true;
  try {
    const res = await api.get('/admin/audit/list', { params: { status: status.value } });
    list.value = res.data.data || [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载审核列表失败');
  } finally {
    loading.value = false;
  }
}

async function load() {
  try {
    await Promise.all([loadOverview(), loadList()]);
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载审核台失败');
  }
}

async function approve(id: number) {
  try {
    await api.post(`/admin/audit/${id}/approve`);
    ElMessage.success('审核通过');
    await load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '审核通过失败');
  }
}

async function openPreview(row: any) {
  try {
    const res = await api.get(`/content/${row.id}`, { params: { incrementView: false } });
    previewItem.value = res.data.data || row;
  } catch (_) {
    previewItem.value = row;
  }
  previewVisible.value = true;
}

function openReject(row: any) {
  currentId.value = row.id;
  rejectReason.value = '';
  rejectVisible.value = true;
}

async function rejectConfirm() {
  if (!currentId.value) return;
  try {
    await api.post(`/admin/audit/${currentId.value}/reject`, { reason: rejectReason.value });
    rejectVisible.value = false;
    ElMessage.success('内容已驳回');
    await load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '驳回失败');
  }
}

function openOffline(row: any) {
  currentId.value = row.id;
  offlineReason.value = '';
  offlineVisible.value = true;
}

async function offlineConfirm() {
  if (!currentId.value) return;
  try {
    await api.post(`/admin/audit/${currentId.value}/offline`, { reason: offlineReason.value });
    offlineVisible.value = false;
    ElMessage.success('内容已下架');
    await load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '下架失败');
  }
}

onMounted(load);
</script>

<style scoped>
.admin-audit-page {
  display: grid;
  gap: 18px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
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
  color: #dc2626;
  background: rgba(239, 68, 68, 0.12);
  font-size: 12px;
  font-weight: 700;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 16px;
}

.summary-card,
.panel-card {
  border: none;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 24px 50px rgba(15, 23, 42, 0.08);
}

.summary-card {
  padding: 18px;
}

.summary-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.summary-card strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
  color: #0f172a;
}

.toolbar {
  margin-bottom: 16px;
}

.empty-wrap {
  margin-top: 18px;
}

.preview-meta {
  color: #64748b;
}

.preview-summary {
  color: #475569;
}

.preview-body {
  margin-top: 14px;
  color: #0f172a;
  line-height: 1.8;
}

@media (max-width: 1080px) {
  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .summary-grid {
    grid-template-columns: 1fr;
  }
}
</style>

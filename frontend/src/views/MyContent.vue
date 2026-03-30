<template>
  <div class="my-content-page">
    <div class="page-head">
      <div>
        <div class="section-tag">作者中心</div>
        <h2>我的内容</h2>
        <p>支持按状态筛选、进入编辑、查看审核记录和删除内容，便于完整展示作者侧创作管理流程。</p>
      </div>
      <el-button type="primary" @click="$router.push('/publish')">发布新内容</el-button>
    </div>

    <div class="toolbar">
      <el-select v-model="status" placeholder="内容状态" @change="load">
        <el-option label="全部状态" value="" />
        <el-option label="已发布" value="PUBLISHED" />
        <el-option label="草稿" value="DRAFT" />
        <el-option label="待审核" value="PENDING_REVIEW" />
        <el-option label="已驳回" value="REJECTED" />
        <el-option label="已下架" value="OFFLINE" />
      </el-select>
    </div>

    <div v-loading="loading" class="content-list">
      <article v-for="row in list" :key="row.id" class="content-card">
        <div class="content-main">
          <div class="content-meta">
            <el-tag size="small" effect="plain">{{ typeLabel(row.type) }}</el-tag>
            <el-tag size="small" :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </div>
          <h3>{{ row.title }}</h3>
          <p>{{ row.summary || '该内容暂无摘要，进入详情页可查看完整正文。' }}</p>
          <div class="stat-row">
            <span>阅读 {{ row.viewCount || 0 }}</span>
            <span>点赞 {{ row.likeCount || 0 }}</span>
            <span>收藏 {{ row.favoriteCount || 0 }}</span>
          </div>
        </div>
        <div class="content-actions">
          <el-button size="small" @click="$router.push(`/content/${row.id}`)">查看</el-button>
          <el-button size="small" type="primary" plain @click="$router.push(`/publish?id=${row.id}`)">编辑</el-button>
          <el-button size="small" @click="showAuditLogs(row)">审核记录</el-button>
          <el-popconfirm title="确认删除这条内容吗？" @confirm="remove(row.id)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </article>
    </div>

    <div v-if="!loading && list.length === 0" class="empty-wrap">
      <el-empty description="当前筛选条件下暂无内容。" />
    </div>

    <el-dialog v-model="auditVisible" title="审核记录" width="720px">
      <el-timeline v-if="auditLogs.length > 0">
        <el-timeline-item v-for="log in auditLogs" :key="log.id" :timestamp="log.createdAt">
          <div><strong>{{ log.action }}</strong></div>
          <div class="audit-reason">{{ log.reason || '暂无补充说明' }}</div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无审核记录" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import api from '../api';

const list = ref<any[]>([]);
const loading = ref(false);
const auditVisible = ref(false);
const auditLogs = ref<any[]>([]);
const status = ref('');

function typeLabel(value?: string) {
  if (value === 'QUESTION') return '问答';
  if (value === 'TUTORIAL') return '教程';
  return '文章';
}

function statusLabel(value?: string) {
  if (value === 'PUBLISHED') return '已发布';
  if (value === 'DRAFT') return '草稿';
  if (value === 'PENDING_REVIEW') return '待审核';
  if (value === 'REJECTED') return '已驳回';
  if (value === 'OFFLINE') return '已下架';
  return value || '未知状态';
}

function statusTagType(value?: string) {
  if (value === 'PUBLISHED') return 'success';
  if (value === 'PENDING_REVIEW') return 'warning';
  if (value === 'REJECTED') return 'danger';
  if (value === 'OFFLINE') return 'info';
  return '';
}

async function load() {
  loading.value = true;
  try {
    const res = await api.get('/content/mine', { params: { status: status.value || undefined } });
    list.value = res.data.data || [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载我的内容失败');
  } finally {
    loading.value = false;
  }
}

async function showAuditLogs(row: any) {
  try {
    const res = await api.get(`/content/${row.id}/audit-logs`);
    auditLogs.value = res.data.data || [];
    auditVisible.value = true;
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载审核记录失败');
  }
}

async function remove(id: number) {
  try {
    await api.delete(`/content/${id}`);
    ElMessage.success('删除成功');
    await load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '删除失败');
  }
}

onMounted(load);
</script>

<style scoped>
.my-content-page { display: grid; gap: 18px; }
.page-head { display: flex; justify-content: space-between; align-items: flex-end; gap: 16px; }
.page-head h2 { margin: 8px 0 6px; font-size: 30px; }
.page-head p { margin: 0; color: #64748b; line-height: 1.75; }
.section-tag { display: inline-block; padding: 6px 12px; border-radius: 999px; color: #0f766e; background: rgba(16, 185, 129, .12); font-size: 12px; font-weight: 700; }
.toolbar { display: flex; justify-content: flex-end; }
.content-list { display: grid; gap: 14px; }
.content-card { display: grid; grid-template-columns: minmax(0, 1fr) 260px; gap: 18px; padding: 20px; border-radius: 22px; background: rgba(255, 255, 255, .92); box-shadow: 0 20px 45px rgba(15, 23, 42, .08); }
.content-meta, .stat-row { display: flex; gap: 10px; flex-wrap: wrap; }
.content-main h3 { margin: 14px 0 10px; color: #0f172a; }
.content-main p { margin: 0; color: #475569; line-height: 1.8; }
.stat-row { margin-top: 14px; color: #64748b; font-size: 13px; }
.content-actions { display: flex; gap: 10px; justify-content: flex-end; align-items: flex-start; flex-wrap: wrap; }
.audit-reason { margin-top: 6px; color: #64748b; }
@media (max-width: 860px) { .page-head, .content-card { display: grid; grid-template-columns: 1fr; } .toolbar { justify-content: stretch; } }
</style>

<template>
  <div class="notice-page">
    <section class="notice-hero">
      <div>
        <span class="hero-badge">站内通知</span>
        <h1>消息中心</h1>
        <p>集中查看评论、回复、审核、最佳答案和新内容提醒，并支持未读筛选与一键已读。</p>
      </div>
    </section>

    <section class="notice-panel">
      <div class="panel-head">
        <div>
          <h2>最新通知</h2>
          <p>点击通知可以直接跳转到关联内容，适合演示二期通知闭环。</p>
        </div>
        <div class="toolbar">
          <el-switch v-model="unreadOnly" active-text="仅看未读" @change="load" />
          <el-button type="primary" plain @click="markAllRead">全部已读</el-button>
        </div>
      </div>

      <div v-loading="loading" class="notice-list">
        <article v-for="n in notifications" :key="n.id" class="notice-card" @click="openNotice(n)">
          <div class="notice-main">
            <div class="notice-top">
              <h3>{{ n.title }}</h3>
              <el-tag :type="n.isRead ? 'info' : 'danger'" effect="light">{{ n.isRead ? '已读' : '未读' }}</el-tag>
            </div>
            <p v-if="n.content">{{ n.content }}</p>
            <span class="notice-time">{{ formatTime(n.createdAt) }}</span>
          </div>
          <el-button
            v-if="!n.isRead"
            type="primary"
            plain
            :loading="marking === n.id"
            :disabled="marking === n.id"
            @click.stop="markRead(n.id)"
          >
            标记已读
          </el-button>
        </article>
      </div>

      <div v-if="!loading && notifications.length === 0" class="empty-wrap">
        <el-empty description="暂时还没有通知。" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import api from '../api';

const router = useRouter();
const notifications = ref<any[]>([]);
const loading = ref(false);
const marking = ref<number | null>(null);
const unreadOnly = ref(false);

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

async function load() {
  loading.value = true;
  try {
    const res = await api.get('/notification/mine', {
      params: { unreadOnly: unreadOnly.value || undefined }
    });
    notifications.value = res.data.data || [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载通知失败');
  } finally {
    loading.value = false;
  }
}

async function markRead(id: number) {
  marking.value = id;
  try {
    await api.post(`/notification/${id}/read`);
    await load();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '标记通知失败');
  } finally {
    marking.value = null;
  }
}

async function markAllRead() {
  try {
    await api.post('/notification/read-all');
    await load();
    ElMessage.success('已全部标记为已读');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '全部已读失败');
  }
}

function openNotice(item: any) {
  if (!item?.relatedId) return;
  router.push(item.type === 'BEST_ANSWER' ? `/qa/${item.relatedId}` : `/content/${item.relatedId}`);
}

onMounted(load);
</script>

<style scoped>
.notice-page { max-width:1120px; margin:0 auto; padding:28px 20px 40px; display:grid; gap:22px; }
.notice-hero, .notice-panel { border-radius:28px; background:rgba(255,255,255,.92); box-shadow:0 20px 45px rgba(15,23,42,.08); }
.notice-hero { padding:30px 32px; background: radial-gradient(circle at top right, rgba(249,115,22,.18), transparent 36%), linear-gradient(135deg, rgba(255,247,237,.96), rgba(255,255,255,.92)); }
.hero-badge { display:inline-block; padding:6px 12px; border-radius:999px; background:rgba(194,65,12,.1); color:#c2410c; font-size:12px; font-weight:700; }
.notice-hero h1 { margin:16px 0 10px; }
.notice-hero p { margin:0; color:#475569; line-height:1.8; }
.notice-panel { padding:24px; }
.panel-head { display:flex; justify-content:space-between; align-items:center; gap:16px; }
.panel-head h2 { margin:0 0 8px; }
.panel-head p { margin:0; color:#64748b; }
.toolbar { display:flex; gap:12px; align-items:center; }
.notice-list { display:grid; gap:14px; margin-top:18px; }
.notice-card { display:flex; justify-content:space-between; gap:18px; align-items:center; padding:18px 20px; border-radius:20px; border:1px solid rgba(148,163,184,.18); background:linear-gradient(180deg,#fff 0%,#fbfdff 100%); cursor:pointer; }
.notice-main { flex:1; min-width:0; }
.notice-top { display:flex; justify-content:space-between; gap:12px; align-items:center; }
.notice-top h3 { margin:0; color:#0f172a; font-size:17px; }
.notice-main p { margin:8px 0 0; color:#475569; line-height:1.7; }
.notice-time { display:block; margin-top:10px; color:#94a3b8; font-size:13px; }
.empty-wrap { margin-top:18px; }
@media (max-width: 760px) { .panel-head, .notice-card, .notice-top { flex-direction:column; align-items:flex-start; } }
</style>

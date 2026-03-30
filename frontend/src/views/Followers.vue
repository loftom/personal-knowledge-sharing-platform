<template>
  <div class="follow-page">
    <div class="page-head">
      <div>
        <div class="section-tag">社交关系</div>
        <h2>粉丝与关注</h2>
        <p>查看当前账号的关注列表与粉丝列表，形成个人空间的关系展示闭环。</p>
      </div>
    </div>

    <div class="follow-grid" v-loading="loading">
      <section class="follow-card">
        <h3>我关注的人</h3>
        <div v-if="following.length > 0" class="user-list">
          <div v-for="u in following" :key="u.id" class="user-row">
            <div class="user-avatar">{{ (u.nickname || u.username || 'U').slice(0, 1) }}</div>
            <div>
              <div class="user-name">{{ u.nickname || u.username }}</div>
              <div class="user-sub">@{{ u.username }}</div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂时还没有关注任何人。" />
      </section>

      <section class="follow-card">
        <h3>我的粉丝</h3>
        <div v-if="followers.length > 0" class="user-list">
          <div v-for="u in followers" :key="u.id" class="user-row">
            <div class="user-avatar">{{ (u.nickname || u.username || 'U').slice(0, 1) }}</div>
            <div>
              <div class="user-name">{{ u.nickname || u.username }}</div>
              <div class="user-sub">@{{ u.username }}</div>
            </div>
          </div>
        </div>
        <el-empty v-else description="暂时还没有粉丝。" />
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import api from '../api';

const followers = ref<any[]>([]);
const following = ref<any[]>([]);
const loading = ref(false);

async function load() {
  loading.value = true;
  try {
    const [followersRes, followingRes] = await Promise.all([
      api.get('/follow/followers'),
      api.get('/follow/following')
    ]);
    followers.value = followersRes.data.data || [];
    following.value = followingRes.data.data || [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载关注数据失败');
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.follow-page { display:grid; gap:18px; }
.page-head h2 { margin:8px 0 6px; font-size:30px; }
.page-head p { margin:0; color:#64748b; }
.section-tag { display:inline-block; padding:6px 12px; border-radius:999px; color:#7c3aed; background:rgba(124,58,237,.12); font-size:12px; font-weight:700; }
.follow-grid { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:18px; }
.follow-card { padding:22px; border-radius:28px; background:rgba(255,255,255,.9); box-shadow:0 24px 50px rgba(15,23,42,.08); }
.follow-card h3 { margin:0 0 14px; }
.user-list { display:grid; gap:12px; }
.user-row { display:flex; align-items:center; gap:12px; padding:12px 14px; border-radius:16px; background:#f8fbff; }
.user-avatar { display:inline-flex; width:40px; height:40px; align-items:center; justify-content:center; border-radius:50%; color:#fff; font-weight:700; background:linear-gradient(135deg,#2563eb,#0f766e); }
.user-name { color:#0f172a; font-weight:700; }
.user-sub { color:#64748b; font-size:12px; }
@media (max-width: 860px) { .follow-grid { grid-template-columns:1fr; } }
</style>

<template>
  <div class="profile-page">
    <section class="profile-hero">
      <el-skeleton v-if="loading" :rows="3" animated />
      <template v-else-if="profile">
        <div class="hero-main">
          <div>
            <span class="hero-badge">个人主页</span>
            <h1>{{ profile.name }}</h1>
            <p>展示作者的公开内容、粉丝规模和持续创作表现，便于进一步关注与交流。</p>
          </div>
          <div class="hero-actions" v-if="canFollow">
            <el-button type="primary" :loading="followLoading" @click="toggleFollow">
              {{ following ? '取消关注' : '关注作者' }}
            </el-button>
          </div>
        </div>

        <div class="profile-stats">
          <div class="stat-box">
            <span>发布内容</span>
            <strong>{{ profile.postCount }}</strong>
          </div>
          <div class="stat-box">
            <span>粉丝数量</span>
            <strong>{{ profile.followerCount }}</strong>
          </div>
        </div>
      </template>
    </section>

    <section class="profile-panel">
      <div class="panel-head">
        <div>
          <h2>公开内容</h2>
          <p>点击卡片即可进入内容详情页。</p>
        </div>
      </div>

      <div v-if="profile" class="works-list">
        <article v-for="row in profile.posts || []" :key="row.id" class="work-card" @click="$router.push(`/content/${row.id}`)">
          <div class="work-meta">
            <el-tag size="small" effect="plain">{{ row.type }}</el-tag>
            <span>{{ row.status }}</span>
          </div>
          <h3>{{ row.title }}</h3>
          <div class="work-stats">
            <span>阅读 {{ row.viewCount || 0 }}</span>
            <span>点赞 {{ row.likeCount || 0 }}</span>
          </div>
        </article>
      </div>

      <div v-if="!loading && (!profile || !profile.posts || profile.posts.length === 0)" class="empty-wrap">
        <el-empty description="这位作者暂时还没有公开内容。" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '../api';

const route = useRoute();
const id = route.params.id as string | undefined;

const profile = ref<any>(null);
const loading = ref(false);
const followLoading = ref(false);
const following = ref(false);

const currentUserId = Number(localStorage.getItem('userId') || 0);
const targetUserId = computed(() => Number(id || currentUserId || 0));
const canFollow = computed(() => !!id && currentUserId > 0 && targetUserId.value !== currentUserId);

function sanitizeDisplayName(rawNickname?: string, rawUsername?: string) {
  const nicknameValue = (rawNickname || '').trim();
  if (nicknameValue && !/^[?？]+$/.test(nicknameValue) && !/^锟?$/.test(nicknameValue)) {
    return nicknameValue;
  }
  return (rawUsername || '').trim() || '未命名用户';
}

async function loadFollowState() {
  if (!canFollow.value) {
    following.value = false;
    return;
  }
  try {
    const res = await api.get('/follow/following');
    const list = res.data.data || [];
    following.value = list.some((item: any) => Number(item.id) === targetUserId.value);
  } catch (_) {
    following.value = false;
  }
}

async function load() {
  loading.value = true;
  try {
    const url = id ? `/profile/${id}/space` : '/profile/me/space';
    const res = await api.get(url);
    const data = res.data.data || null;
    profile.value = data
      ? {
          id: data.user?.id,
          name: sanitizeDisplayName(data.user?.nickname, data.user?.username),
          postCount: data.stats?.contentCount || 0,
          followerCount: data.stats?.followerCount || 0,
          posts: data.works || []
        }
      : null;
    await loadFollowState();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载主页失败');
  } finally {
    loading.value = false;
  }
}

async function toggleFollow() {
  if (!localStorage.getItem('token')) {
    ElMessage.warning('请先登录后再关注作者');
    return;
  }
  followLoading.value = true;
  try {
    const res = await api.post(`/follow/${targetUserId.value}/toggle`);
    following.value = !!res.data.data?.following;
    if (profile.value) {
      profile.value.followerCount += following.value ? 1 : -1;
      if (profile.value.followerCount < 0) profile.value.followerCount = 0;
    }
    ElMessage.success(following.value ? '关注成功' : '已取消关注');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '关注操作失败');
  } finally {
    followLoading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.profile-page { max-width:1120px; margin:0 auto; padding:28px 20px 40px; display:grid; gap:22px; }
.profile-hero, .profile-panel { border-radius:28px; background:rgba(255,255,255,.92); box-shadow:0 20px 45px rgba(15,23,42,.08); }
.profile-hero { padding:30px 32px; background: radial-gradient(circle at top right, rgba(16,185,129,.15), transparent 38%), linear-gradient(135deg, rgba(236,253,245,.95), rgba(255,255,255,.96)); }
.hero-main { display:flex; justify-content:space-between; gap:18px; align-items:flex-start; }
.hero-badge { display:inline-block; padding:6px 12px; border-radius:999px; background:rgba(4,120,87,.08); color:#047857; font-size:12px; font-weight:700; }
.hero-main h1 { margin:16px 0 10px; }
.hero-main p { margin:0; color:#475569; line-height:1.8; }
.profile-stats { display:grid; grid-template-columns:repeat(2,minmax(0,1fr)); gap:14px; margin-top:20px; }
.stat-box { padding:16px; border-radius:18px; background:rgba(255,255,255,.86); }
.stat-box span { display:block; color:#64748b; font-size:12px; }
.stat-box strong { display:block; margin-top:8px; color:#0f172a; font-size:28px; }
.profile-panel { padding:24px; }
.panel-head h2 { margin:0 0 8px; }
.panel-head p { margin:0; color:#64748b; }
.works-list { display:grid; gap:14px; margin-top:18px; }
.work-card { padding:18px 20px; border-radius:20px; border:1px solid rgba(148,163,184,.18); background:linear-gradient(180deg,#fff 0%,#fbfdff 100%); cursor:pointer; }
.work-meta, .work-stats { display:flex; gap:10px; flex-wrap:wrap; color:#64748b; font-size:13px; }
.work-card h3 { margin:12px 0 10px; color:#0f172a; }
@media (max-width: 760px) { .hero-main, .profile-stats { flex-direction:column; grid-template-columns:1fr; } }
</style>

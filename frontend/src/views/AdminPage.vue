<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="sidebar-brand">
        <div class="brand-badge"></div>
        <div>
          <div class="brand-title">管理后台</div>
          <small>Knowledge Platform Console</small>
        </div>
      </div>

      <div v-if="!isAdmin" class="sidebar-warning">
        当前账号不是管理员，无法访问后台管理功能。
      </div>

      <template v-else>
        <div class="menu-group">
          <div class="menu-title">工作台</div>
          <button class="menu-item" :class="{ active: activeView === 'overview' }" @click="activeView = 'overview'">
            数据概览
          </button>
        </div>

        <div class="menu-group">
          <div class="menu-title">内容治理</div>
          <button class="menu-item" :class="{ active: activeView === 'audit' }" @click="activeView = 'audit'">
            审核管理
          </button>
          <button class="menu-item" :class="{ active: activeView === 'taxonomy' }" @click="activeView = 'taxonomy'">
            分类标签
          </button>
        </div>

        <div class="menu-group">
          <div class="menu-title">用户洞察</div>
          <button class="menu-item" :class="{ active: activeView === 'preference' }" @click="activeView = 'preference'">
            用户偏好
          </button>
          <button class="menu-item" :class="{ active: activeView === 'manage' }" @click="activeView = 'manage'">
            用户与内容管理
          </button>
        </div>

        <div class="sidebar-footer">
          <el-button class="back-btn" plain @click="router.push('/')">返回前台</el-button>
        </div>
      </template>
    </aside>

    <main class="admin-main">
      <header class="admin-topbar">
        <div>
          <div class="breadcrumb">首页 / {{ currentLabel }}</div>
          <h1>{{ currentTitle }}</h1>
        </div>
        <div class="topbar-user">
          <span class="user-dot"></span>
          <span>{{ displayName }}</span>
        </div>
      </header>

      <section v-if="!isAdmin" class="access-panel">
        <el-alert title="当前账号不是管理员，请切换管理员账号后再访问后台。" type="warning" :closable="false" />
      </section>

      <template v-else>
        <section v-if="activeView === 'overview'" class="overview-page">
          <div class="metric-grid">
            <article v-for="item in headlineMetrics" :key="item.key" class="metric-card" :class="item.className">
              <div class="metric-icon"></div>
              <div class="metric-value">{{ item.value }}</div>
              <div class="metric-label">{{ item.label }}</div>
              <div class="metric-sub">{{ item.sub }}</div>
            </article>
          </div>

          <div class="content-grid">
            <section class="dashboard-panel">
              <div class="panel-head">
                <div>
                  <h3>内容状态分布</h3>
                  <p>用于判断当前审核积压、已发布规模和下架治理压力。</p>
                </div>
                <el-button text @click="loadDashboard">刷新</el-button>
              </div>

              <div class="progress-list">
                <div v-for="item in statusDistribution" :key="item.label" class="progress-item">
                  <div class="progress-info">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                  <div class="progress-track">
                    <span class="progress-bar" :style="{ width: `${item.percent}%`, background: item.color }"></span>
                  </div>
                </div>
              </div>
            </section>

            <section class="dashboard-panel">
              <div class="panel-head">
                <div>
                  <h3>内容互动分布</h3>
                  <p>这里统计的是已发布内容的浏览、获赞、收藏和评论。</p>
                </div>
              </div>

              <div class="progress-list">
                <div v-for="item in engagementDistribution" :key="item.label" class="progress-item">
                  <div class="progress-info">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                  </div>
                  <div class="progress-track">
                    <span class="progress-bar" :style="{ width: `${item.percent}%`, background: item.color }"></span>
                  </div>
                </div>
              </div>
            </section>
          </div>

          <div class="content-grid secondary-grid">
            <section class="dashboard-panel">
              <div class="panel-head">
                <div>
                  <h3>内容组织概况</h3>
                  <p>查看分类体系、标签体量和当前最活跃的偏好主题。</p>
                </div>
              </div>

              <div class="taxonomy-summary">
                <div class="taxonomy-card">
                  <strong>{{ taxonomySummary.categoryCount }}</strong>
                  <span>分类总数</span>
                </div>
                <div class="taxonomy-card">
                  <strong>{{ taxonomySummary.tagCount }}</strong>
                  <span>标签总数</span>
                </div>
                <div class="taxonomy-card">
                  <strong>{{ preferenceSummary.topTag }}</strong>
                  <span>最活跃偏好标签</span>
                </div>
              </div>
            </section>

            <section class="dashboard-panel">
              <div class="panel-head">
                <div>
                  <h3>用户与内容概况</h3>
                  <p>结合画像用户数、内容总数和下架内容量，快速了解平台当前状态。</p>
                </div>
              </div>

              <div class="taxonomy-summary">
                <div class="taxonomy-card">
                  <strong>{{ preferenceSummary.userCount }}</strong>
                  <span>画像用户数</span>
                </div>
                <div class="taxonomy-card">
                  <strong>{{ dashboard.totalContentCount || 0 }}</strong>
                  <span>内容总数</span>
                </div>
                <div class="taxonomy-card">
                  <strong>{{ overview.offlineCount || 0 }}</strong>
                  <span>当前下架内容</span>
                </div>
              </div>
            </section>
          </div>
        </section>

        <section v-else class="module-panel">
          <AdminAuditPage v-if="activeView === 'audit'" />
          <AdminPreferencePage v-else-if="activeView === 'preference'" />
          <AdminManagePage v-else-if="activeView === 'manage'" />
          <TaxonomyManage v-else />
        </section>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import api from '../api';
import { getAdminAuth, hasAdminSession } from '../utils/auth';
import TaxonomyManage from './TaxonomyManage.vue';
import AdminAuditPage from './AdminAuditPage.vue';
import AdminPreferencePage from './AdminPreferencePage.vue';
import AdminManagePage from './AdminManagePage.vue';

type AdminView = 'overview' | 'audit' | 'preference' | 'taxonomy' | 'manage';

const router = useRouter();
const isAdmin = computed(() => hasAdminSession());
const displayName = computed(() => getAdminAuth().nickname || getAdminAuth().username || '管理员');
const activeView = ref<AdminView>('overview');
const overview = ref<Record<string, number>>({});
const dashboard = ref<Record<string, number>>({});
const preferenceList = ref<any[]>([]);
const categories = ref<any[]>([]);
const tags = ref<any[]>([]);

const currentLabel = computed(() => {
  if (activeView.value === 'audit') return '审核管理';
  if (activeView.value === 'preference') return '用户偏好';
  if (activeView.value === 'manage') return '用户与内容管理';
  if (activeView.value === 'taxonomy') return '分类标签';
  return '数据概览';
});

const currentTitle = computed(() => {
  if (activeView.value === 'audit') return '内容审核中心';
  if (activeView.value === 'preference') return '用户偏好洞察';
  if (activeView.value === 'manage') return '用户与内容管理';
  if (activeView.value === 'taxonomy') return '分类与标签管理';
  return '后台数据概览';
});

const headlineMetrics = computed(() => [
  {
    key: 'users',
    className: 'metric-blue',
    value: Number(dashboard.value.totalUserCount || 0),
    label: '用户数',
    sub: `画像用户 ${preferenceSummary.value.userCount}`
  },
  {
    key: 'content',
    className: 'metric-pink',
    value: Number(dashboard.value.publishedContentCount || 0),
    label: '已发布内容',
    sub: `文章 ${Number(dashboard.value.articleCount || 0)} · 问答 ${Number(dashboard.value.questionCount || 0)}`
  },
  {
    key: 'views',
    className: 'metric-cyan',
    value: Number(dashboard.value.totalViewCount || 0),
    label: '内容浏览量',
    sub: `内容获赞 ${Number(dashboard.value.totalLikeCount || 0)} · 内容收藏 ${Number(dashboard.value.totalFavoriteCount || 0)}`
  },
  {
    key: 'engagement',
    className: 'metric-green',
    value: Number(dashboard.value.totalCommentCount || 0),
    label: '评论总数',
    sub: `关注关系 ${Number(dashboard.value.totalFollowerCount || 0)}`
  }
]);

const statusDistribution = computed(() => {
  const items = [
    { label: '待审核', value: Number(overview.value.pendingCount || 0), color: 'linear-gradient(90deg, #f59e0b 0%, #fb7185 100%)' },
    { label: '已发布', value: Number(overview.value.publishedCount || 0), color: 'linear-gradient(90deg, #06b6d4 0%, #3b82f6 100%)' },
    { label: '已下架', value: Number(overview.value.offlineCount || 0), color: 'linear-gradient(90deg, #10b981 0%, #22c55e 100%)' }
  ];
  const max = Math.max(...items.map((item) => item.value), 1);
  return items.map((item) => ({
    ...item,
    percent: Math.max(Math.round((item.value / max) * 100), item.value > 0 ? 12 : 0)
  }));
});

const engagementDistribution = computed(() => {
  const items = [
    { label: '内容浏览量', value: Number(dashboard.value.totalViewCount || 0), color: 'linear-gradient(90deg, #0ea5e9 0%, #2563eb 100%)' },
    { label: '内容获赞数', value: Number(dashboard.value.totalLikeCount || 0), color: 'linear-gradient(90deg, #8b5cf6 0%, #6366f1 100%)' },
    { label: '内容收藏数', value: Number(dashboard.value.totalFavoriteCount || 0), color: 'linear-gradient(90deg, #ec4899 0%, #f97316 100%)' },
    { label: '评论总数', value: Number(dashboard.value.totalCommentCount || 0), color: 'linear-gradient(90deg, #14b8a6 0%, #22c55e 100%)' }
  ];
  const max = Math.max(...items.map((item) => item.value), 1);
  return items.map((item) => ({
    ...item,
    percent: Math.max(Math.round((item.value / max) * 100), item.value > 0 ? 12 : 0)
  }));
});

const preferenceSummary = computed(() => {
  const userCount = preferenceList.value.length;
  const tagCounter = new Map<string, number>();
  preferenceList.value.forEach((user) => {
    (user.preferences || []).forEach((tag: any) => {
      const key = tag?.tagName || '未分类';
      tagCounter.set(key, (tagCounter.get(key) || 0) + 1);
    });
  });
  const topTag = [...tagCounter.entries()].sort((a, b) => b[1] - a[1])[0]?.[0] || '暂无数据';
  return { userCount, topTag };
});

const taxonomySummary = computed(() => ({
  categoryCount: categories.value.length,
  tagCount: tags.value.length
}));

async function loadDashboard() {
  if (!isAdmin.value) {
    return;
  }
  try {
    const [dashboardRes, overviewRes, preferenceRes, categoryRes, tagRes] = await Promise.all([
      api.get('/admin/audit/dashboard'),
      api.get('/admin/audit/overview'),
      api.get('/admin/audit/preferences'),
      api.get('/public/taxonomy/categories'),
      api.get('/public/taxonomy/tags')
    ]);
    dashboard.value = dashboardRes.data.data || {};
    overview.value = overviewRes.data.data || {};
    preferenceList.value = preferenceRes.data.data || [];
    categories.value = categoryRes.data.data || [];
    tags.value = tagRes.data.data || [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载后台概览失败');
  }
}

onMounted(loadDashboard);
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 268px minmax(0, 1fr);
  background: #edf5f9;
}

.admin-sidebar {
  display: flex;
  flex-direction: column;
  gap: 26px;
  padding: 26px 20px;
  color: #d5e8f6;
  background: linear-gradient(180deg, #183245 0%, #112a3d 100%);
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.04);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 10px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-badge {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: linear-gradient(135deg, #10b981 0%, #2563eb 100%);
}

.brand-title {
  font-size: 30px;
  font-weight: 800;
  color: #fff;
}

.sidebar-brand small {
  color: rgba(213, 232, 246, 0.72);
}

.menu-group {
  display: grid;
  gap: 8px;
}

.menu-title {
  padding: 0 10px;
  color: rgba(213, 232, 246, 0.58);
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.menu-item {
  width: 100%;
  padding: 14px 16px;
  border: none;
  border-radius: 16px;
  text-align: left;
  font-size: 16px;
  font-weight: 700;
  color: #d5e8f6;
  background: transparent;
  cursor: pointer;
  transition: 0.2s ease;
}

.menu-item:hover,
.menu-item.active {
  color: #fff;
  background: rgba(59, 130, 246, 0.2);
  box-shadow: inset 0 0 0 1px rgba(125, 211, 252, 0.18);
}

.sidebar-footer {
  margin-top: auto;
}

.back-btn {
  width: 100%;
}

.sidebar-warning {
  padding: 16px;
  border-radius: 18px;
  line-height: 1.7;
  color: #fff0b3;
  background: rgba(245, 158, 11, 0.16);
}

.admin-main {
  min-width: 0;
  display: grid;
  grid-template-rows: auto 1fr;
}

.admin-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 24px 28px;
  background: rgba(255, 255, 255, 0.92);
  border-bottom: 1px solid rgba(15, 23, 42, 0.06);
}

.admin-topbar h1 {
  margin: 8px 0 0;
  color: #0f172a;
  font-size: 30px;
}

.breadcrumb {
  color: #64748b;
  font-size: 14px;
  font-weight: 600;
}

.topbar-user {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  border-radius: 999px;
  color: #475569;
  font-weight: 700;
  background: #f8fbfd;
}

.user-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: linear-gradient(135deg, #22c55e 0%, #06b6d4 100%);
}

.access-panel,
.overview-page,
.module-panel {
  padding: 28px;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.metric-card {
  padding: 22px;
  border-radius: 28px;
  color: #0f172a;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
}

.metric-icon {
  width: 56px;
  height: 56px;
  border-radius: 18px;
  margin-bottom: 20px;
}

.metric-blue .metric-icon {
  background: linear-gradient(135deg, #5b7cff 0%, #6d28d9 100%);
}

.metric-pink .metric-icon {
  background: linear-gradient(135deg, #ff6aa8 0%, #ff4d4f 100%);
}

.metric-cyan .metric-icon {
  background: linear-gradient(135deg, #0ea5e9 0%, #22d3ee 100%);
}

.metric-green .metric-icon {
  background: linear-gradient(135deg, #22c55e 0%, #34d399 100%);
}

.metric-value {
  font-size: 46px;
  font-weight: 800;
  line-height: 1;
}

.metric-label {
  margin-top: 14px;
  color: #475569;
  font-size: 16px;
  font-weight: 700;
}

.metric-sub {
  margin-top: 8px;
  color: #16a34a;
  font-size: 14px;
}

.content-grid {
  display: grid;
  grid-template-columns: 1.15fr 0.85fr;
  gap: 18px;
  margin-top: 18px;
}

.secondary-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.dashboard-panel {
  padding: 24px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.08);
}

.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: start;
  gap: 16px;
  margin-bottom: 18px;
}

.panel-head h3 {
  margin: 0;
  font-size: 28px;
  color: #0f172a;
}

.panel-head p {
  margin: 8px 0 0;
  color: #64748b;
  line-height: 1.7;
}

.progress-list {
  display: grid;
  gap: 18px;
}

.progress-item {
  display: grid;
  gap: 10px;
}

.progress-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  color: #334155;
  font-weight: 700;
}

.progress-track {
  width: 100%;
  height: 12px;
  overflow: hidden;
  border-radius: 999px;
  background: #e8eef3;
}

.progress-bar {
  display: block;
  height: 100%;
  border-radius: inherit;
}

.taxonomy-summary {
  display: grid;
  gap: 14px;
}

.taxonomy-card {
  display: grid;
  gap: 8px;
  padding: 18px 20px;
  border-radius: 22px;
  background: linear-gradient(135deg, #f8fbfd 0%, #eef6fb 100%);
}

.taxonomy-card strong {
  color: #0f172a;
  font-size: 34px;
  font-weight: 800;
}

.taxonomy-card span {
  color: #64748b;
  font-weight: 600;
}

@media (max-width: 1280px) {
  .metric-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .content-grid,
  .secondary-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    gap: 16px;
  }
}

@media (max-width: 720px) {
  .admin-topbar,
  .overview-page,
  .module-panel {
    padding: 18px;
  }

  .metric-grid {
    grid-template-columns: 1fr;
  }

  .admin-topbar {
    flex-direction: column;
    align-items: start;
  }
}
</style>

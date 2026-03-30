<template>
  <div class="growth-page" v-loading="loading">
    <div class="page-head">
      <div>
        <div class="section-tag">三期成长体系</div>
        <h2>积分等级与勋章</h2>
        <p>展示积分余额、等级进度、勋章解锁情况和最近成长记录，方便论文里说明激励机制闭环。</p>
      </div>
    </div>

    <div v-if="report" class="growth-grid">
      <div class="hero-card">
        <div class="metric-box">
          <span>积分余额</span>
          <strong>{{ pointOverview.balance }}</strong>
        </div>
        <div class="metric-box">
          <span>当前等级</span>
          <strong>Lv.{{ pointOverview.levelNo }}</strong>
        </div>
        <div class="progress-card">
          <div class="progress-top">
            <span>升级进度</span>
            <strong>{{ progressText }}</strong>
          </div>
          <el-progress :percentage="progressPercent" :stroke-width="14" :show-text="false" />
          <p>{{ progressHint }}</p>
        </div>
      </div>

      <div class="panel-card">
        <h3>已获得勋章</h3>
        <div v-if="badgeList.length > 0" class="badge-list">
          <div v-for="badge in badgeList" :key="badge.code" class="badge-chip">
            <strong>{{ badge.label }}</strong>
            <span>{{ badge.desc }}</span>
          </div>
        </div>
        <el-empty v-else description="当前还没有获得勋章，继续发布与互动即可逐步解锁。" />
      </div>
    </div>

    <el-card class="panel-card" shadow="never">
      <h3>升级说明</h3>
      <div class="level-list">
        <div v-for="item in levels" :key="item.level" class="level-row">
          <span>Lv.{{ item.level }}</span>
          <strong>{{ item.range }}</strong>
          <small>{{ item.desc }}</small>
        </div>
      </div>
    </el-card>

    <el-card class="panel-card" shadow="never">
      <h3>最近成长记录</h3>
      <el-timeline v-if="recentLogs.length > 0">
        <el-timeline-item v-for="item in recentLogs" :key="item.id" :timestamp="formatTime(item.createdAt)">
          <div class="log-line">
            <strong :class="{ gain: item.changeAmount > 0, loss: item.changeAmount < 0 }">
              {{ item.changeAmount > 0 ? '+' : '' }}{{ item.changeAmount }}
            </strong>
            <span>{{ reasonLabel(item.reason) }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂时还没有积分变动记录。" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import dayjs from 'dayjs';
import { ElMessage } from 'element-plus';
import api from '../api';

const report = ref<any>(null);
const loading = ref(false);

const levels = [
  { level: 1, range: '0 - 29 积分', desc: '完成基础注册与初次发布' },
  { level: 2, range: '30 - 99 积分', desc: '开始形成稳定创作和互动' },
  { level: 3, range: '100 - 199 积分', desc: '进入优质作者成长阶段' },
  { level: 4, range: '200+ 积分', desc: '成为社区高活跃核心贡献者' }
];

const pointOverview = computed(() => report.value?.points || { balance: 0, levelNo: 1, badges: [] });
const recentLogs = computed(() => report.value?.recentPointLogs || []);
const badgeList = computed(() => (pointOverview.value.badges || []).map((badge: string) => mapBadge(badge)));

const progressPercent = computed(() => {
  const balance = Number(pointOverview.value.balance || 0);
  if (balance >= 200) {
    return 100;
  }
  if (balance >= 100) {
    return Math.round(((balance - 100) / 100) * 100);
  }
  if (balance >= 30) {
    return Math.round(((balance - 30) / 70) * 100);
  }
  return Math.round((balance / 30) * 100);
});

const progressText = computed(() => {
  const balance = Number(pointOverview.value.balance || 0);
  if (balance >= 200) {
    return '已达到最高等级';
  }
  const nextTarget = balance >= 100 ? 200 : balance >= 30 ? 100 : 30;
  return `${balance} / ${nextTarget}`;
});

const progressHint = computed(() => {
  const balance = Number(pointOverview.value.balance || 0);
  if (balance >= 200) {
    return '你已经达到当前成长体系的最高等级，可以继续积累影响力数据。';
  }
  const nextTarget = balance >= 100 ? 200 : balance >= 30 ? 100 : 30;
  return `距离下一级还差 ${nextTarget - balance} 积分。`;
});

function mapBadge(name: string) {
  const mapping: Record<string, { code: string; label: string; desc: string }> = {
    'Active Creator': { code: 'ACTIVE_CREATOR', label: '持续创作者', desc: '累计积分达到 30，完成稳定创作起步。' },
    'Quality Author': { code: 'QUALITY_AUTHOR', label: '优质作者', desc: '累计积分达到 100，说明内容持续获得正向反馈。' },
    'Community Star': { code: 'COMMUNITY_STAR', label: '社区之星', desc: '累计积分达到 200，具备较强社区影响力。' }
  };
  return mapping[name] || { code: name, label: name, desc: '已获得的一项成长勋章。' };
}

function reasonLabel(reason?: string) {
  const mapping: Record<string, string> = {
    PUBLISH_APPROVED: '内容审核通过',
    CONTENT_LIKED: '内容收到点赞',
    BEST_ANSWER: '回答被采纳为最佳答案',
    CONTENT_OFFLINE: '内容违规下架扣分'
  };
  return mapping[reason || ''] || reason || '未知积分变动';
}

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

async function load() {
  loading.value = true;
  try {
    const res = await api.get('/profile/me/report');
    report.value = res.data.data || null;
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载成长数据失败');
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.growth-page {
  display: grid;
  gap: 18px;
}

.page-head h2,
.panel-card h3 {
  margin: 8px 0 6px;
}

.page-head p {
  margin: 0;
  color: #64748b;
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

.growth-grid {
  display: grid;
  grid-template-columns: 1.1fr 0.9fr;
  gap: 18px;
}

.hero-card,
.panel-card {
  border-radius: 28px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.9);
  box-shadow: 0 24px 50px rgba(15, 23, 42, 0.08);
}

.hero-card {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  background: linear-gradient(135deg, rgba(15, 118, 110, 0.94), rgba(37, 99, 235, 0.92));
}

.metric-box,
.progress-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.14);
  color: #fff;
}

.progress-card {
  grid-column: 1 / -1;
}

.metric-box span,
.progress-top span {
  display: block;
  font-size: 13px;
}

.metric-box strong {
  display: block;
  margin-top: 10px;
  font-size: 30px;
}

.progress-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
}

.progress-card p {
  margin: 10px 0 0;
  color: rgba(255, 255, 255, 0.85);
}

.badge-list {
  display: grid;
  gap: 12px;
  margin-top: 10px;
}

.badge-chip {
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(15, 118, 110, 0.08);
  color: #0f172a;
}

.badge-chip strong {
  display: block;
}

.badge-chip span {
  display: block;
  margin-top: 6px;
  color: #64748b;
  line-height: 1.7;
}

.level-list {
  display: grid;
  gap: 12px;
}

.level-row {
  display: grid;
  grid-template-columns: 90px 1fr;
  gap: 8px 16px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #f8fbff;
}

.level-row small {
  grid-column: 2;
  color: #64748b;
}

.log-line {
  display: flex;
  gap: 10px;
  align-items: center;
}

.gain {
  color: #16a34a;
}

.loss {
  color: #dc2626;
}

@media (max-width: 900px) {
  .growth-grid,
  .hero-card {
    grid-template-columns: 1fr;
  }
}
</style>

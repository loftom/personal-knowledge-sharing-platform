<template>
  <div class="report-page">
    <div class="report-head">
      <div>
        <div class="section-tag">数据分析</div>
        <h2>个人影响力报告</h2>
        <p>展示当前账号的内容表现、最近七日趋势与积分流水，适合三期能力演示。</p>
      </div>
    </div>

    <section class="report-panel" v-loading="loading">
      <div v-if="report" class="report-body">
        <div class="stat-grid">
          <div class="stat-box">
            <span>已发布内容</span>
            <strong>{{ report.publishedContentCount || 0 }}</strong>
          </div>
          <div class="stat-box">
            <span>总阅读量</span>
            <strong>{{ report.totalViews || 0 }}</strong>
          </div>
          <div class="stat-box">
            <span>影响力分</span>
            <strong>{{ report.influenceScore || 0 }}</strong>
          </div>
          <div class="stat-box">
            <span>积分余额</span>
            <strong>{{ report.points?.balance || 0 }}</strong>
          </div>
        </div>

        <div class="summary-grid">
          <div class="summary-card">
            <h3>核心指标</h3>
            <p>获赞数：{{ report.totalLikes || 0 }}</p>
            <p>收藏数：{{ report.totalFavorites || 0 }}</p>
            <p>收到评论：{{ report.totalCommentsReceived || 0 }}</p>
            <p>粉丝数：{{ report.followerCount || 0 }}</p>
            <p>点赞率：{{ report.likeRate }}</p>
            <p>收藏率：{{ report.favoriteRate }}</p>
          </div>

          <div class="summary-card">
            <h3>成长情况</h3>
            <p>等级：Lv.{{ report.points?.levelNo || 1 }}</p>
            <p>勋章：{{ (report.points?.badges || []).join('、') || '暂无' }}</p>
          </div>
        </div>

        <div class="trend-section">
          <div class="trend-head">
            <h3>最近七日趋势图</h3>
            <p>使用柱状条展示每日阅读、点赞、收藏与积分变化。</p>
          </div>
          <div class="trend-chart">
            <div v-for="item in report.trends || []" :key="item.dateLabel" class="trend-column">
              <div class="bar-group">
                <div class="bar bar-views" :style="{ height: `${calcHeight(item.views, maxViews)}px` }"></div>
                <div class="bar bar-likes" :style="{ height: `${calcHeight(item.likes, maxLikes)}px` }"></div>
                <div class="bar bar-favorites" :style="{ height: `${calcHeight(item.favorites, maxFavorites)}px` }"></div>
                <div class="bar bar-points" :style="{ height: `${calcHeight(item.pointDelta, maxPoints)}px` }"></div>
              </div>
              <div class="trend-date">{{ item.dateLabel.slice(5) }}</div>
              <div class="trend-meta">发 {{ item.publishedCount }} / 阅 {{ item.views }}</div>
            </div>
          </div>
          <div class="legend-row">
            <span><i class="legend-dot views"></i>阅读</span>
            <span><i class="legend-dot likes"></i>点赞</span>
            <span><i class="legend-dot favorites"></i>收藏</span>
            <span><i class="legend-dot points"></i>积分</span>
          </div>
        </div>

        <div class="trend-section">
          <h3>最近积分流水</h3>
          <el-timeline v-if="(report.recentPointLogs || []).length > 0">
            <el-timeline-item v-for="item in report.recentPointLogs" :key="item.id" :timestamp="formatTime(item.createdAt)">
              <div class="point-log-line">
                <strong :class="{ gain: item.changeAmount > 0, loss: item.changeAmount < 0 }">
                  {{ item.changeAmount > 0 ? '+' : '' }}{{ item.changeAmount }}
                </strong>
                <span>{{ item.reason }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无积分流水" />
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import api from '../api';

const report = ref<any>(null);
const loading = ref(false);

const trends = computed(() => report.value?.trends || []);
const maxViews = computed(() => Math.max(...trends.value.map((item: any) => item.views || 0), 1));
const maxLikes = computed(() => Math.max(...trends.value.map((item: any) => item.likes || 0), 1));
const maxFavorites = computed(() => Math.max(...trends.value.map((item: any) => item.favorites || 0), 1));
const maxPoints = computed(() => Math.max(...trends.value.map((item: any) => item.pointDelta || 0), 1));

function calcHeight(value: number, max: number) {
  if (!value || !max) return 10;
  return Math.max(10, Math.round((value / max) * 120));
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
    ElMessage.error(e?.response?.data?.message || e.message || '加载报告失败');
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.report-page { display:grid; gap:18px; }
.report-head h2 { margin:8px 0 6px; font-size:30px; }
.report-head p { margin:0; color:#64748b; }
.section-tag { display:inline-block; padding:6px 12px; border-radius:999px; color:#1d4ed8; background:rgba(59,130,246,.12); font-size:12px; font-weight:700; }
.report-panel { border-radius:28px; background:rgba(255,255,255,.9); box-shadow:0 24px 50px rgba(15,23,42,.08); padding:22px; }
.report-body { display:grid; gap:22px; }
.stat-grid, .summary-grid { display:grid; gap:16px; }
.stat-grid { grid-template-columns:repeat(4,minmax(0,1fr)); }
.summary-grid { grid-template-columns:repeat(2,minmax(0,1fr)); }
.stat-box, .summary-card, .trend-section { padding:18px; border-radius:20px; background:linear-gradient(180deg,#ffffff 0%,#f8fbff 100%); box-shadow:inset 0 0 0 1px rgba(148,163,184,.14); }
.stat-box span { color:#64748b; font-size:13px; }
.stat-box strong { display:block; margin-top:10px; color:#0f172a; font-size:28px; }
.summary-card h3, .trend-section h3 { margin:0 0 12px; }
.summary-card p { margin:8px 0; color:#475569; }
.trend-head { display:flex; justify-content:space-between; gap:12px; align-items:end; margin-bottom:14px; }
.trend-head p { margin:0; color:#64748b; }
.trend-chart { display:grid; grid-template-columns:repeat(7,minmax(0,1fr)); gap:12px; align-items:end; min-height:180px; }
.trend-column { display:flex; flex-direction:column; align-items:center; gap:8px; }
.bar-group { width:100%; min-height:132px; display:flex; align-items:end; justify-content:center; gap:6px; }
.bar { width:16px; border-radius:999px 999px 6px 6px; }
.bar-views { background:linear-gradient(180deg,#38bdf8,#0ea5e9); }
.bar-likes { background:linear-gradient(180deg,#f59e0b,#f97316); }
.bar-favorites { background:linear-gradient(180deg,#a78bfa,#7c3aed); }
.bar-points { background:linear-gradient(180deg,#22c55e,#16a34a); }
.trend-date { color:#0f172a; font-weight:700; }
.trend-meta { color:#64748b; font-size:12px; }
.legend-row { display:flex; gap:16px; flex-wrap:wrap; margin-top:14px; color:#475569; font-size:13px; }
.legend-dot { display:inline-block; width:10px; height:10px; border-radius:50%; margin-right:6px; }
.views { background:#0ea5e9; } .likes { background:#f97316; } .favorites { background:#7c3aed; } .points { background:#16a34a; }
.point-log-line { display:flex; gap:10px; align-items:center; }
.gain { color:#16a34a; } .loss { color:#dc2626; }
@media (max-width: 920px) {
  .stat-grid, .summary-grid { grid-template-columns:1fr; }
  .trend-chart { grid-template-columns:repeat(4,minmax(0,1fr)); }
  .trend-head { flex-direction:column; align-items:flex-start; }
}
</style>

<template>
  <div class="recommend-page">
    <section class="recommend-hero">
      <div>
        <span class="hero-badge">个性化推荐</span>
        <h1>推荐内容</h1>
        <p>
          本页面基于用户兴趣标签、近期互动行为与内容热度综合生成推荐结果，
          用于展示平台在内容发现与个性化分发方面的实现能力。
        </p>
      </div>
    </section>

    <section class="recommend-panel">
      <div class="panel-head">
        <div>
          <h2>推荐列表</h2>
          <p>系统将优先呈现与用户关注方向、浏览行为和社区活跃度更匹配的内容。</p>
        </div>
      </div>

      <div v-loading="loading" class="recommend-list">
        <article v-for="item in list" :key="item.id" class="recommend-card" @click="openDetail(item)">
          <div class="recommend-main">
            <div class="recommend-meta">
              <el-tag size="small" effect="plain" :type="item.type === 'QUESTION' ? 'warning' : 'success'">
                {{ item.type === 'QUESTION' ? '问答' : item.type === 'TUTORIAL' ? '教程' : '文章' }}
              </el-tag>
              <span>作者 {{ item.authorName || item.authorId || '匿名用户' }}</span>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.summary || '系统已将该内容纳入推荐结果，进入详情页可查看完整正文。' }}</p>
            <div class="reason-line">推荐依据：兴趣标签匹配、内容热度、发布时间与近期互动表现</div>
          </div>

          <div class="recommend-side">
            <div class="stat-card">
              <span>阅读</span>
              <strong>{{ item.viewCount || 0 }}</strong>
            </div>
            <div class="stat-card">
              <span>点赞</span>
              <strong>{{ item.likeCount || 0 }}</strong>
            </div>
            <div class="stat-card">
              <span>收藏</span>
              <strong>{{ item.favoriteCount || 0 }}</strong>
            </div>
          </div>
        </article>
      </div>

      <div v-if="!loading && list.length === 0" class="empty-wrap">
        <el-empty description="当前暂无推荐内容，请先浏览、点赞或收藏部分内容后再查看推荐结果。" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '../api';

const router = useRouter();
const list = ref<any[]>([]);
const loading = ref(false);

function openDetail(row: any) {
  router.push(row.type === 'QUESTION' ? `/qa/${row.id}` : `/content/${row.id}`);
}

async function load() {
  loading.value = true;
  try {
    const res = await api.get('/recommend/feed');
    list.value = (res.data.data || []).map((item: any) => ({
      ...item,
      id: item.contentId,
      authorName: item.authorName || item.nickname || item.username || item.authorId
    }));
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载推荐失败');
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.recommend-page {
  max-width: 1120px;
  margin: 0 auto;
  padding: 28px 20px 40px;
  display: grid;
  gap: 22px;
}

.recommend-hero,
.recommend-panel {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.08);
}

.recommend-hero {
  padding: 30px 32px;
  background:
    radial-gradient(circle at top right, rgba(16, 185, 129, 0.18), transparent 38%),
    linear-gradient(135deg, rgba(236, 253, 245, 0.95), rgba(239, 246, 255, 0.96));
}

.hero-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.08);
  color: #0f766e;
  font-size: 12px;
  font-weight: 700;
}

.recommend-hero h1 {
  margin: 16px 0 10px;
  color: #0f172a;
}

.recommend-hero p {
  margin: 0;
  color: #475569;
  line-height: 1.8;
}

.recommend-panel {
  padding: 24px;
}

.panel-head h2 {
  margin: 0 0 8px;
}

.panel-head p {
  margin: 0;
  color: #64748b;
  line-height: 1.75;
}

.recommend-list {
  display: grid;
  gap: 16px;
  margin-top: 18px;
}

.recommend-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 18px;
  padding: 20px;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(180deg, #fff 0%, #fbfdff 100%);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.recommend-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
}

.recommend-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: #64748b;
  font-size: 13px;
}

.recommend-main h3 {
  margin: 14px 0 10px;
  color: #0f172a;
  font-size: 24px;
}

.recommend-main p {
  margin: 0;
  color: #475569;
  line-height: 1.8;
}

.reason-line {
  margin-top: 12px;
  color: #0f766e;
  font-size: 13px;
  font-weight: 600;
}

.recommend-side {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  align-self: start;
}

.stat-card {
  padding: 14px 10px;
  text-align: center;
  border-radius: 16px;
  background: #f0fdf4;
}

.stat-card span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.stat-card strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
}

.empty-wrap {
  padding-top: 18px;
}

@media (max-width: 860px) {
  .recommend-card {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="home-page">
    <section class="home-hero">
      <div class="hero-copy">
        <span class="hero-badge">知识分享与社区协作平台</span>
        <h1>知识内容广场</h1>
        <p>
          本页面集中展示平台内已公开发布的文章、教程与问答内容，
          支持按关键词、分类、标签和排序方式进行检索与筛选，
          便于从内容发现、阅读互动到个人创作形成完整使用闭环。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="$router.push('/publish')">发布内容</el-button>
          <el-button size="large" @click="$router.push('/qa')">进入问答区</el-button>
        </div>
      </div>
    </section>

    <section class="home-panel">
      <div class="panel-top">
        <div>
          <h2>内容列表</h2>
          <p>以信息流方式展示社区内容，突出标题、摘要、作者与互动指标，提升浏览与筛选效率。</p>
        </div>
        <div class="search-box">
          <el-input
            v-model="keyword"
            placeholder="搜索标题、摘要或正文关键词"
            clearable
            @input="onKeywordChange"
          />
        </div>
      </div>

      <div class="filter-row">
        <el-select v-model="categoryId" clearable placeholder="全部分类" @change="load">
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="tagId" clearable placeholder="全部标签" @change="load">
          <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="sortBy" placeholder="排序方式" @change="load">
          <el-option v-for="item in sortOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="起始日期"
          end-placeholder="结束日期"
          format="YYYY-MM-DD"
          value-format="YYYY-MM-DD"
          :shortcuts="dateShortcuts"
          style="width: 260px"
          @change="load"
        />
      </div>

      <div v-loading="loading" class="feed-list">
        <article
          v-for="item in list"
          :key="item.id"
          class="feed-card"
          @click="$router.push(item.type === 'QUESTION' ? `/qa/${item.id}` : `/content/${item.id}`)"
        >
          <div class="feed-main">
            <div class="feed-meta">
              <el-tag size="small" effect="plain" :type="item.type === 'QUESTION' ? 'warning' : 'primary'">
                {{ item.type === 'QUESTION' ? '问答' : item.type === 'TUTORIAL' ? '教程' : '文章' }}
              </el-tag>
              <span>作者 {{ item.authorName || item.authorId || '匿名用户' }}</span>
              <span>{{ formatTime(item.publishedAt || item.createdAt) }}</span>
            </div>
            <h3>{{ item.title }}</h3>
            <p>{{ item.summary || '该内容暂无摘要，进入详情页可查看完整正文。' }}</p>
          </div>

          <div class="feed-side">
            <div class="metric">
              <span>阅读</span>
              <strong>{{ item.viewCount || 0 }}</strong>
            </div>
            <div class="metric">
              <span>点赞</span>
              <strong>{{ item.likeCount || 0 }}</strong>
            </div>
            <div class="metric">
              <span>收藏</span>
              <strong>{{ item.favoriteCount || 0 }}</strong>
            </div>
          </div>
        </article>
      </div>

      <div v-if="!loading && list.length === 0" class="empty-wrap">
        <el-empty description="当前筛选条件下暂无内容，请尝试调整检索条件后重试。" />
      </div>

      <div v-if="totalPages > 1" class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          :page-size="pageSize"
          :total="totalCount"
          :pager-count="5"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import dayjs from 'dayjs';
import api from '../api';

const keyword = ref('');
const categoryId = ref<number | undefined>();
const tagId = ref<number | undefined>();
const sortBy = ref('publishedAt');
const dateRange = ref<[string, string] | null>(null);
const page = ref(1);
const pageSize = ref(20);
const totalCount = ref(0);
const totalPages = ref(0);
const list = ref<any[]>([]);
const categories = ref<any[]>([]);
const tags = ref<any[]>([]);
const loading = ref(false);
let searchTimer: ReturnType<typeof setTimeout> | null = null;

const sortOptions = [
  { label: '按最新发布', value: 'publishedAt' },
  { label: '按阅读量', value: 'views' },
  { label: '按点赞量', value: 'likes' },
  { label: '按收藏量', value: 'favorites' }
];

const dateShortcuts = [
  { text: '最近一周', value: () => { const d = new Date(); const w = new Date(d.getTime() - 7 * 86400000); return [w, d]; } },
  { text: '最近一月', value: () => { const d = new Date(); const m = new Date(d.getTime() - 30 * 86400000); return [m, d]; } },
];

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function sanitizeTaxonomyName(value: string) {
  const text = (value || '').trim();
  if (!text || /[�]/.test(text)) return '未命名';
  return text;
}

async function loadTaxonomy() {
  const [categoryRes, tagRes] = await Promise.all([
    api.get('/public/taxonomy/categories'),
    api.get('/public/taxonomy/tags')
  ]);
  categories.value = (categoryRes.data.data || []).map((item: any) => ({
    ...item,
    name: sanitizeTaxonomyName(item.name)
  }));
  tags.value = (tagRes.data.data || []).map((item: any) => ({
    ...item,
    name: sanitizeTaxonomyName(item.name)
  }));
}

async function load() {
  loading.value = true;
  try {
    const params: any = {
      keyword: keyword.value || undefined,
      categoryId: categoryId.value,
      tagId: tagId.value,
      sortBy: sortBy.value,
      page: page.value,
      size: pageSize.value
    };
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0] + ' 00:00:00';
      params.endDate = dateRange.value[1] + ' 23:59:59';
    }
    const res = await api.get('/public/search', { params });
    const data = res.data.data;
    // Handle new SearchResult format (items + totalCount + totalPages)
    if (data && data.items !== undefined) {
      list.value = data.items.map((item: any) => ({
        ...item,
        authorName: item.authorName || item.nickname || item.username || item.authorId
      }));
      totalCount.value = data.totalCount || 0;
      totalPages.value = data.totalPages || 0;
    } else {
      // Fallback for old flat array response
      list.value = (Array.isArray(data) ? data : []).map((item: any) => ({
        ...item,
        authorName: item.authorName || item.nickname || item.username || item.authorId
      }));
      totalCount.value = list.value.length;
      totalPages.value = 1;
    }
  } finally {
    loading.value = false;
  }
}

function onKeywordChange() {
  if (searchTimer) clearTimeout(searchTimer);
  searchTimer = setTimeout(load, 300);
}

onMounted(async () => {
  await loadTaxonomy();
  await load();
});
</script>

<style scoped>
.home-page {
  display: grid;
  gap: 24px;
}

.home-hero,
.home-panel {
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
}

.home-hero {
  padding: 32px;
  background:
    radial-gradient(circle at top left, rgba(22, 119, 255, 0.16), transparent 34%),
    linear-gradient(135deg, #ffffff 0%, #f5f9ff 60%, #f8fbff 100%);
}

.hero-badge {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(22, 119, 255, 0.08);
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
}

.hero-copy h1 {
  margin: 16px 0 12px;
  font-size: 40px;
  line-height: 1.15;
  color: #0f172a;
}

.hero-copy p {
  margin: 0;
  max-width: 780px;
  color: #475569;
  line-height: 1.85;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.home-panel {
  padding: 24px;
}

.panel-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 18px;
}

.panel-top h2 {
  margin: 0 0 8px;
  color: #0f172a;
}

.panel-top p {
  margin: 0;
  color: #64748b;
  line-height: 1.75;
}

.search-box {
  width: 320px;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
}

.filter-row > * {
  width: 200px;
}

.feed-list {
  display: grid;
  gap: 16px;
  margin-top: 20px;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

.feed-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 18px;
  padding: 22px;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.16);
  background: linear-gradient(180deg, #fff 0%, #fbfdff 100%);
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.feed-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.08);
}

.feed-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  color: #64748b;
  font-size: 13px;
}

.feed-main h3 {
  margin: 14px 0 10px;
  font-size: 24px;
  line-height: 1.35;
  color: #0f172a;
}

.feed-main p {
  margin: 0;
  color: #475569;
  line-height: 1.8;
}

.feed-side {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  align-self: start;
}

.metric {
  padding: 14px 10px;
  text-align: center;
  border-radius: 16px;
  background: #f8fbff;
}

.metric span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.metric strong {
  display: block;
  margin-top: 8px;
  color: #0f172a;
}

.empty-wrap {
  margin-top: 18px;
}

@media (max-width: 960px) {
  .panel-top,
  .filter-row,
  .feed-card {
    display: grid;
    grid-template-columns: 1fr;
  }

  .search-box {
    width: 100%;
  }
}
</style>

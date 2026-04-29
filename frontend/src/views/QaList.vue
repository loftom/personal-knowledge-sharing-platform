<template>
  <div class="page-shell">
    <section class="hero-card">
      <div class="hero-head">
        <div>
          <p class="eyebrow">社区问答</p>
          <h1>问答广场</h1>
          <p class="hero-desc">
            本页面集中展示平台内的问题内容与解答状态，支持按关键词和问题状态进行检索，
            用于体现问答模块在问题沉淀、答案协作与最佳答案采纳方面的完整流程。
          </p>
        </div>
        <el-button type="primary" size="large" @click="$router.push('/publish')">发布问题</el-button>
      </div>

      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="搜索问题关键词"
          class="search-input"
          clearable
          @input="onKeywordChange"
        />
        <el-select v-model="categoryId" clearable placeholder="全部分类" @change="load">
          <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="tagId" clearable placeholder="全部标签" @change="load">
          <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-select v-model="statusFilter" placeholder="问题状态" @change="applyFilter">
          <el-option label="全部状态" value="ALL" />
          <el-option label="待回答" value="PENDING" />
          <el-option label="已解决" value="RESOLVED" />
        </el-select>
      </div>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <h2>问题列表</h2>
          <p>列表展示问题标题、摘要、提问者、回答数量与状态信息，便于快速进入详情页查看讨论进展。</p>
        </div>
      </div>

      <div class="question-grid" v-loading="loading">
        <article v-for="item in filteredList" :key="item.id" class="question-card">
          <div class="question-top">
            <div>
              <a class="question-title" @click.prevent="$router.push(`/qa/${item.id}`)">{{ item.title }}</a>
              <p class="question-summary">{{ item.summary || '该问题暂无摘要，进入详情页可查看完整问题描述。' }}</p>
            </div>
            <el-tag :type="statusTagType(item.status)" effect="light">{{ statusLabel(item.status) }}</el-tag>
          </div>

          <div class="question-meta">
            <span>提问者：{{ item.authorName || `用户 ${item.authorId}` }}</span>
            <span>回答 {{ item.answerCount || 0 }}</span>
            <span>阅读 {{ item.viewCount || 0 }}</span>
            <span>{{ formatTime(item.createdAt) }}</span>
          </div>

          <div class="question-foot">
            <el-tag v-if="item.bestAnswerId" type="success">已采纳最佳答案</el-tag>
            <el-button text type="primary" @click="$router.push(`/qa/${item.id}`)">查看详情</el-button>
          </div>
        </article>
      </div>

      <div v-if="!loading && filteredList.length === 0" class="empty-wrap">
        <el-empty description="当前检索条件下暂无问题结果，请调整关键词或状态后重试。" />
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import api from '../api';

const keyword = ref('');
const categoryId = ref<number | undefined>();
const tagId = ref<number | undefined>();
const rawList = ref<any[]>([]);
const categories = ref<Array<{ id: number; name: string }>>([]);
const tags = ref<Array<{ id: number; name: string }>>([]);
const loading = ref(false);
const statusFilter = ref('ALL');
let searchTimer: number | null = null;

const filteredList = computed(() => {
  if (statusFilter.value === 'ALL') return rawList.value;
  return rawList.value.filter((item) => item.status === statusFilter.value);
});

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function statusLabel(status?: string) {
  return status === 'RESOLVED' ? '已解决' : '待回答';
}

function statusTagType(status?: string) {
  return status === 'RESOLVED' ? 'success' : 'warning';
}

function sanitizeTaxonomyName(value: string) {
  return (value || '').trim() || '未命名';
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
    const res = await api.get('/qa/questions', {
      params: {
        keyword: keyword.value || undefined,
        categoryId: categoryId.value,
        tagId: tagId.value
      }
    });
    rawList.value = res.data.data || [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载问答列表失败');
  } finally {
    loading.value = false;
  }
}

function onKeywordChange() {
  if (searchTimer) window.clearTimeout(searchTimer);
  searchTimer = window.setTimeout(load, 350);
}

function applyFilter() {
  // 状态筛选在前端 computed 中完成，无需额外操作
}

onMounted(async () => {
  await loadTaxonomy();
  await load();
});
</script>

<style scoped>
.page-shell {
  max-width: 1120px;
  margin: 0 auto;
  padding: 28px 20px 40px;
}

.hero-card,
.panel {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 28px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.08);
}

.hero-card {
  padding: 28px 32px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.18), transparent 36%),
    linear-gradient(135deg, rgba(239, 246, 255, 0.98), rgba(255, 255, 255, 0.95));
}

.hero-head {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.eyebrow {
  margin: 0 0 10px;
  color: #1d4ed8;
  font-size: 13px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  color: #0f172a;
}

.hero-desc,
.panel-header p {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.75;
}

.toolbar {
  display: flex;
  gap: 14px;
  margin-top: 18px;
  align-items: center;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 260px;
}

.panel {
  margin-top: 22px;
  padding: 24px;
}

.panel-header {
  margin-bottom: 16px;
}

.question-grid {
  display: grid;
  gap: 16px;
}

.question-card {
  padding: 20px;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.94), rgba(255, 255, 255, 0.98));
}

.question-top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.question-title {
  color: #0f172a;
  font-size: 20px;
  font-weight: 700;
  text-decoration: none;
  cursor: pointer;
}

.question-title:hover {
  color: #1d4ed8;
}

.question-summary {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.7;
}

.question-meta,
.question-foot {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  align-items: center;
}

.question-meta {
  margin-top: 14px;
  color: #64748b;
  font-size: 13px;
}

.question-foot {
  margin-top: 14px;
  justify-content: space-between;
}

.empty-wrap {
  padding: 28px 0 8px;
}

@media (max-width: 720px) {
  .hero-head,
  .question-top {
    flex-direction: column;
  }
}
</style>

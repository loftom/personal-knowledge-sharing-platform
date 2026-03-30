<template>
  <div class="detail-page">
    <el-skeleton v-if="loading" :rows="8" animated class="skeleton-card" />

    <template v-else-if="content">
      <div class="detail-layout">
        <main class="content-column">
          <article class="article-shell">
            <header class="article-header">
              <div class="header-top">
                <span class="type-badge">{{ typeLabel(content.type) }}</span>
                <span class="publish-time">{{ formatTime(content.publishedAt || content.createdAt) }}</span>
              </div>

              <h1 class="article-title">{{ content.title }}</h1>

              <div class="author-strip">
                <div class="author-avatar">{{ authorInitial }}</div>
                <div class="author-text">
                  <strong>{{ content.authorName || `用户 ${content.authorId}` }}</strong>
                  <span>发布于 {{ formatTime(content.publishedAt || content.createdAt) }}</span>
                </div>
              </div>

              <p v-if="content.summary" class="article-summary">
                {{ content.summary }}
              </p>
            </header>

            <section class="article-body" v-html="content.body"></section>
          </article>

          <section class="comment-shell">
            <div class="section-head">
              <div>
                <h2>评论区</h2>
                <p>围绕文章观点展开交流，持续沉淀更完整的讨论内容。</p>
              </div>
              <span class="comment-count">{{ comments.length }} 条评论</span>
            </div>

            <el-alert
              v-if="!isLoggedIn"
              title="当前未登录，可浏览评论内容；登录后可参与评论、回复、点赞与收藏。"
              type="info"
              :closable="false"
              class="login-alert"
            />

            <div class="editor-shell">
              <el-input
                v-model="commentBody"
                type="textarea"
                :rows="5"
                resize="none"
                placeholder="写下你的观点、补充信息或使用感受"
                :disabled="!isLoggedIn"
              />
              <div class="editor-footer">
                <span>文明交流，鼓励围绕主题展开高质量讨论。</span>
                <el-button type="primary" :disabled="!isLoggedIn" @click="submitComment">发布评论</el-button>
              </div>
            </div>

            <div v-if="rootComments.length" class="comment-list">
              <article v-for="comment in rootComments" :key="comment.id" class="comment-item">
                <div class="comment-main">
                  <div class="comment-avatar">
                    {{ (comment.displayName || `用户 ${comment.userId}`).slice(0, 1).toUpperCase() }}
                  </div>

                  <div class="comment-content">
                    <div class="comment-head">
                      <div class="comment-meta">
                        <strong>{{ comment.displayName || `用户 ${comment.userId}` }}</strong>
                        <span>{{ formatTime(comment.createdAt) }}</span>
                      </div>
                      <el-button text type="primary" :disabled="!isLoggedIn" @click="toggleReply(comment.id)">
                        {{ replyingTo === comment.id ? '取消回复' : '回复' }}
                      </el-button>
                    </div>

                    <div class="comment-body">{{ comment.body }}</div>

                    <div v-if="replyingTo === comment.id" class="reply-editor">
                      <el-input
                        v-model="replyBody"
                        type="textarea"
                        :rows="3"
                        resize="none"
                        placeholder="继续补充你的回复内容"
                        :disabled="!isLoggedIn"
                      />
                      <div class="reply-actions">
                        <el-button size="small" :disabled="!isLoggedIn" @click="toggleReply(comment.id)">取消</el-button>
                        <el-button type="primary" size="small" :disabled="!isLoggedIn" @click="submitReply(comment.id)">
                          提交回复
                        </el-button>
                      </div>
                    </div>

                    <div v-if="childrenMap[comment.id]?.length" class="reply-list">
                      <div v-for="reply in childrenMap[comment.id]" :key="reply.id" class="reply-item">
                        <div class="reply-line">
                          <strong>{{ reply.displayName || `用户 ${reply.userId}` }}</strong>
                          <span>{{ formatTime(reply.createdAt) }}</span>
                        </div>
                        <div class="reply-text">{{ reply.body }}</div>
                      </div>
                    </div>
                  </div>
                </div>
              </article>
            </div>

            <div v-else class="empty-wrap">
              <el-empty description="当前还没有评论，欢迎发表第一条交流内容" />
            </div>
          </section>
        </main>

        <aside class="aside-column">
          <section class="author-card">
            <div class="author-card-top">
              <div class="author-card-avatar">{{ authorInitial }}</div>
              <div>
                <div class="author-card-name">{{ content.authorName || `用户 ${content.authorId}` }}</div>
                <div class="author-card-subtitle">内容作者</div>
              </div>
            </div>
            <el-button plain class="profile-button" @click="goAuthorProfile">查看作者主页</el-button>
          </section>

          <section class="stats-card">
            <h3>内容数据</h3>
            <div class="stats-grid">
              <div class="stats-item">
                <span>阅读量</span>
                <strong>{{ content.viewCount || 0 }}</strong>
              </div>
              <div class="stats-item">
                <span>点赞数</span>
                <strong>{{ content.likeCount || 0 }}</strong>
              </div>
              <div class="stats-item">
                <span>收藏数</span>
                <strong>{{ content.favoriteCount || 0 }}</strong>
              </div>
            </div>
          </section>

          <section class="action-card">
            <h3>互动操作</h3>
            <p>支持点赞与收藏，帮助优质内容持续获得更多关注。</p>
            <div class="action-buttons">
              <el-button type="primary" @click="toggleLike">{{ liked ? '取消点赞' : '点赞支持' }}</el-button>
              <el-button plain @click="toggleFavorite">{{ favorited ? '取消收藏' : '加入收藏' }}</el-button>
            </div>
          </section>
        </aside>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import api from '../api';

const route = useRoute();
const router = useRouter();
const id = route.params.id as string;

const content = ref<any>(null);
const comments = ref<any[]>([]);
const commentBody = ref('');
const replyBody = ref('');
const replyingTo = ref<number | null>(null);
const loading = ref(false);
const liked = ref(false);
const favorited = ref(false);
const isLoggedIn = computed(() => !!localStorage.getItem('token'));

const rootComments = computed(() => comments.value.filter((item) => !item.parentId || item.parentId === 0));
const childrenMap = computed(() => {
  const grouped: Record<number, any[]> = {};
  comments.value
    .filter((item) => item.parentId && item.parentId > 0)
    .forEach((item) => {
      const key = Number(item.parentId);
      if (!grouped[key]) grouped[key] = [];
      grouped[key].push(item);
    });
  return grouped;
});

const authorInitial = computed(() => {
  const name = (content.value?.authorName || '').trim();
  return name ? name.slice(0, 1).toUpperCase() : 'U';
});

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function typeLabel(type?: string) {
  if (type === 'QUESTION') return '问答';
  if (type === 'TUTORIAL') return '教程';
  return '文章';
}

function requireLogin() {
  if (isLoggedIn.value) return true;
  ElMessage.warning('请先登录后再进行互动');
  router.push('/login');
  return false;
}

function toggleReply(commentId: number) {
  replyingTo.value = replyingTo.value === commentId ? null : commentId;
  replyBody.value = '';
}

function goAuthorProfile() {
  if (!content.value?.authorId) return;
  router.push(`/profile/${content.value.authorId}`);
}

async function loadDetail(incrementView = true, silent = false) {
  if (!silent) {
    loading.value = true;
  }
  try {
    const [contentRes, commentRes] = await Promise.all([
      api.get(`/content/${id}`, { params: { incrementView } }),
      api.get(`/interaction/comment/${id}`)
    ]);
    content.value = contentRes.data.data;
    liked.value = !!content.value?.liked;
    favorited.value = !!content.value?.favorited;
    comments.value = commentRes.data.data || [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载内容失败');
  } finally {
    if (!silent) {
      loading.value = false;
    }
  }
}

async function submitComment() {
  if (!requireLogin()) return;
  if (!commentBody.value.trim()) {
    ElMessage.warning('请输入评论内容');
    return;
  }
  try {
    await api.post(`/interaction/comment/${id}`, { body: commentBody.value });
    commentBody.value = '';
    await loadDetail(false, true);
    ElMessage.success('评论发布成功');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '评论发布失败');
  }
}

async function submitReply(parentId: number) {
  if (!requireLogin()) return;
  if (!replyBody.value.trim()) {
    ElMessage.warning('请输入回复内容');
    return;
  }
  try {
    await api.post(`/interaction/comment/${id}`, { body: replyBody.value, parentId });
    replyBody.value = '';
    replyingTo.value = null;
    await loadDetail(false, true);
    ElMessage.success('回复提交成功');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '回复提交失败');
  }
}

async function toggleLike() {
  if (!requireLogin()) return;
  try {
    const res = await api.post('/interaction/like/toggle', { targetId: Number(id), targetType: 'CONTENT' });
    liked.value = !!res.data.data?.liked;
    await loadDetail(false, true);
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '点赞操作失败');
  }
}

async function toggleFavorite() {
  if (!requireLogin()) return;
  try {
    const res = await api.post(`/interaction/favorite/${id}/toggle`);
    favorited.value = !!res.data.data?.favorited;
    await loadDetail(false, true);
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '收藏操作失败');
  }
}

onMounted(loadDetail);
</script>

<style scoped>
.detail-page {
  max-width: 1180px;
  margin: 0 auto;
  padding: 24px 20px 48px;
}

.detail-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px;
  gap: 24px;
  align-items: start;
}

.article-shell,
.comment-shell,
.author-card,
.stats-card,
.action-card,
.skeleton-card {
  background: #fff;
  border: 1px solid #e8ecf3;
  border-radius: 18px;
}

.article-shell {
  padding: 36px 40px;
}

.skeleton-card {
  padding: 24px;
}

.header-top {
  display: flex;
  align-items: center;
  gap: 12px;
}

.type-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 52px;
  height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: #eef4ff;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
}

.publish-time {
  color: #8590a6;
  font-size: 13px;
}

.article-title {
  margin: 18px 0 22px;
  color: #121212;
  font-size: 36px;
  line-height: 1.35;
  font-weight: 700;
}

.author-strip {
  display: flex;
  align-items: center;
  gap: 14px;
}

.author-avatar,
.author-card-avatar,
.comment-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: linear-gradient(135deg, #1677ff, #5ea3ff);
  color: #fff;
  font-weight: 700;
}

.author-avatar {
  width: 44px;
  height: 44px;
  font-size: 18px;
}

.author-text {
  display: grid;
  gap: 4px;
}

.author-text strong {
  color: #121212;
  font-size: 15px;
}

.author-text span {
  color: #8590a6;
  font-size: 13px;
}

.article-summary {
  margin: 22px 0 0;
  padding: 16px 18px;
  border-radius: 14px;
  background: #f7f9fc;
  color: #526070;
  line-height: 1.85;
}

.article-body {
  margin-top: 30px;
  color: #1f2329;
  font-size: 17px;
  line-height: 2;
}

.article-body :deep(p) {
  margin: 0 0 18px;
}

.article-body :deep(h2) {
  margin: 32px 0 16px;
  color: #121212;
  font-size: 24px;
  line-height: 1.45;
}

.article-body :deep(blockquote) {
  margin: 20px 0;
  padding: 14px 18px;
  border-left: 4px solid #c7d2fe;
  border-radius: 0 12px 12px 0;
  background: #f8faff;
  color: #526070;
}

.article-body :deep(pre) {
  overflow: auto;
  margin: 22px 0;
  padding: 18px;
  border-radius: 14px;
  background: #0f172a;
  color: #e2e8f0;
}

.article-body :deep(code) {
  font-family: 'Consolas', 'Courier New', monospace;
}

.aside-column {
  position: sticky;
  top: 20px;
  display: grid;
  gap: 16px;
}

.author-card,
.stats-card,
.action-card {
  padding: 20px;
}

.author-card-top {
  display: flex;
  align-items: center;
  gap: 14px;
}

.author-card-avatar {
  width: 48px;
  height: 48px;
  font-size: 18px;
}

.author-card-name {
  color: #121212;
  font-size: 16px;
  font-weight: 700;
}

.author-card-subtitle {
  margin-top: 4px;
  color: #8590a6;
  font-size: 13px;
}

.profile-button {
  width: 100%;
  margin-top: 16px;
}

.stats-card h3,
.action-card h3 {
  margin: 0 0 14px;
  color: #121212;
  font-size: 16px;
}

.stats-grid {
  display: grid;
  gap: 12px;
}

.stats-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid #eef2f7;
}

.stats-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.stats-item span {
  color: #8590a6;
  font-size: 14px;
}

.stats-item strong {
  color: #121212;
  font-size: 24px;
  font-weight: 700;
}

.action-card p {
  margin: 0;
  color: #6b7280;
  line-height: 1.8;
}

.action-buttons {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.action-buttons :deep(.el-button) {
  width: 100%;
  margin-left: 0;
}

.comment-shell {
  margin-top: 20px;
  padding: 28px;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}

.section-head h2 {
  margin: 0;
  color: #121212;
  font-size: 24px;
}

.section-head p {
  margin: 8px 0 0;
  color: #8590a6;
  line-height: 1.75;
}

.comment-count {
  color: #8590a6;
  font-size: 13px;
}

.login-alert {
  margin-top: 18px;
}

.editor-shell {
  margin-top: 18px;
  padding: 18px;
  border: 1px solid #edf1f6;
  border-radius: 16px;
  background: #fafbfd;
}

.editor-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 12px;
  color: #8590a6;
  font-size: 13px;
}

.comment-list {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.comment-item {
  padding: 20px 0;
  border-bottom: 1px solid #eef2f7;
}

.comment-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.comment-main {
  display: flex;
  gap: 14px;
}

.comment-avatar {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  font-size: 15px;
}

.comment-content {
  flex: 1;
}

.comment-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.comment-meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.comment-meta strong,
.reply-line strong {
  color: #121212;
  font-size: 14px;
}

.comment-meta span,
.reply-line span {
  color: #8590a6;
  font-size: 13px;
}

.comment-body,
.reply-text {
  margin-top: 10px;
  color: #1f2329;
  line-height: 1.9;
}

.reply-editor {
  margin-top: 14px;
  padding: 14px;
  border-radius: 14px;
  background: #f8fafc;
}

.reply-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 10px;
}

.reply-list {
  display: grid;
  gap: 10px;
  margin-top: 14px;
  padding-left: 14px;
  border-left: 2px solid #eef2f7;
}

.reply-item {
  padding: 12px 14px;
  border-radius: 14px;
  background: #f8fafc;
}

.reply-line {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.empty-wrap {
  margin-top: 22px;
}

@media (max-width: 980px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }

  .aside-column {
    position: static;
  }

  .article-shell {
    padding: 28px 22px;
  }

  .article-title {
    font-size: 30px;
  }

  .section-head,
  .editor-footer,
  .comment-head {
    display: grid;
    grid-template-columns: 1fr;
  }
}
</style>

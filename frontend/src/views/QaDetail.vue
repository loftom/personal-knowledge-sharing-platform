<template>
  <div class="page-shell">
    <el-skeleton v-if="loading" :rows="6" animated class="skeleton-card" />

    <template v-else-if="question">
      <section class="hero-card">
        <div class="hero-head">
          <div>
            <p class="eyebrow">问题详情</p>
            <h1>{{ question.title }}</h1>
            <div class="hero-meta">
              <span>提问者：{{ question.authorName || `用户 ${question.ownerId}` }}</span>
              <span>问题状态：{{ statusLabel(question.status) }}</span>
              <span v-if="question.bestAnswerId">已采纳最佳答案</span>
            </div>
            <div class="hero-desc" v-html="question.content"></div>
          </div>

          <div class="status-card">
            <span>当前状态</span>
            <strong>{{ statusLabel(question.status) }}</strong>
            <el-tag v-if="question.bestAnswerId" type="success" class="status-tag">最佳答案已确定</el-tag>
            <el-button
              v-if="canReopen"
              type="warning"
              plain
              class="reopen-btn"
              @click="reopenQuestion"
            >
              重新开放问题
            </el-button>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h2>回答列表</h2>
            <p>系统支持对问题回答进行展示、采纳与状态流转，用于体现问答协作场景的完整实现。</p>
          </div>
        </div>

        <div v-if="answers.length" class="answer-list">
          <article v-for="answer in answers" :key="answer.id" :class="['answer-card', { best: answer.isBest }]">
            <div v-if="answer.isBest" class="best-banner">最佳答案</div>
            <div class="answer-body" v-html="answer.body"></div>
            <div class="answer-meta">
              <span>回答者：{{ answer.displayName || answer.nickname || `用户 ${answer.userId}` }}</span>
              <span>点赞 {{ answer.likeCount || 0 }}</span>
              <span>{{ formatTime(answer.createdAt) }}</span>
            </div>
            <div class="answer-actions">
              <el-tag v-if="answer.isBest" type="success" effect="dark">已采纳</el-tag>
              <el-button
                v-else-if="canMarkBest"
                type="primary"
                plain
                :disabled="loading"
                @click="markBest(answer.id)"
              >
                设为最佳答案
              </el-button>
            </div>
          </article>
        </div>

        <div v-else class="empty-wrap">
          <el-empty description="当前问题尚无回答，欢迎补充你的见解与方案。" />
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h2>提交回答</h2>
            <p>支持输入结构化文字内容，用于展示问题分析、解决步骤与补充说明。</p>
          </div>
        </div>

        <el-alert
          v-if="!isLoggedIn"
          title="当前未登录，登录后可参与回答与互动。"
          type="info"
          :closable="false"
          class="login-tip"
        />
        <el-input
          v-model="newAnswer"
          type="textarea"
          :rows="7"
          placeholder="请输入你的回答内容"
          :disabled="!isLoggedIn"
        />
        <div class="submit-bar">
          <el-button type="primary" :loading="submitting" :disabled="submitting || !isLoggedIn" @click="submitAnswer">
            提交回答
          </el-button>
        </div>
      </section>
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

const question = ref<any>(null);
const answers = ref<any[]>([]);
const newAnswer = ref('');
const loading = ref(false);
const submitting = ref(false);

const currentUserId = computed(() => Number(localStorage.getItem('userId')) || null);
const currentRole = computed(() => localStorage.getItem('role') || 'USER');
const isLoggedIn = computed(() => !!localStorage.getItem('token'));
const canMarkBest = computed(() => !!question.value && (question.value.ownerId === currentUserId.value || currentRole.value === 'ADMIN'));
const canReopen = computed(() => canMarkBest.value && question.value?.status === 'RESOLVED');

function formatTime(value?: string) {
  return value ? dayjs(value).format('YYYY-MM-DD HH:mm') : '-';
}

function statusLabel(status?: string) {
  return status === 'RESOLVED' ? '已解决' : '待回答';
}

function ensureLogin() {
  if (isLoggedIn.value) return true;
  ElMessage.warning('请先登录后再进行操作');
  router.push('/login');
  return false;
}

async function load() {
  loading.value = true;
  try {
    const [contentRes, answerRes, stateRes] = await Promise.all([
      api.get(`/content/${id}`),
      api.get(`/qa/${id}/answers`),
      api.get(`/qa/${id}/state`)
    ]);
    const content = contentRes.data.data || null;
    const state = stateRes.data.data || {};
    question.value = content
      ? {
          title: content.title,
          content: content.body,
          ownerId: content.authorId,
          authorName: content.authorName || `用户 ${content.authorId}`,
          status: state.status || 'PENDING',
          bestAnswerId: state.bestAnswerId || null
        }
      : null;
    answers.value = answerRes.data.data || [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载问题详情失败');
  } finally {
    loading.value = false;
  }
}

async function submitAnswer() {
  if (!ensureLogin()) return;
  if (!newAnswer.value.trim()) {
    ElMessage.warning('请输入回答内容');
    return;
  }
  submitting.value = true;
  try {
    await api.post(`/qa/${id}/answer`, { body: newAnswer.value });
    newAnswer.value = '';
    await load();
    ElMessage.success('回答已提交');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '提交回答失败');
  } finally {
    submitting.value = false;
  }
}

async function markBest(answerId: number) {
  try {
    await api.post(`/qa/${id}/best`, { answerId });
    await load();
    ElMessage.success('最佳答案设置成功');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '设置最佳答案失败');
  }
}

async function reopenQuestion() {
  try {
    await api.post(`/qa/${id}/reopen`);
    await load();
    ElMessage.success('问题已重新开放');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '重新开放失败');
  }
}

onMounted(load);
</script>

<style scoped>
.page-shell {
  max-width: 1120px;
  margin: 0 auto;
  padding: 28px 20px 40px;
}

.hero-card,
.panel,
.skeleton-card {
  background: rgba(255, 255, 255, 0.92);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 28px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.08);
}

.hero-card {
  padding: 28px 32px;
  background:
    radial-gradient(circle at top right, rgba(99, 102, 241, 0.18), transparent 36%),
    linear-gradient(135deg, rgba(238, 242, 255, 0.98), rgba(255, 255, 255, 0.94));
}

.skeleton-card {
  padding: 28px 32px;
}

.hero-head {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 220px;
  gap: 20px;
  align-items: start;
}

.eyebrow {
  margin: 0 0 10px;
  color: #4338ca;
  font-size: 13px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

h1,
h2 {
  margin: 0;
  color: #0f172a;
}

.hero-meta,
.answer-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  color: #64748b;
  font-size: 13px;
}

.hero-meta {
  margin: 14px 0 0;
}

.hero-desc {
  margin: 16px 0 0;
  color: #475569;
  line-height: 1.8;
}

.status-card {
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.status-card span {
  display: block;
  color: #64748b;
  font-size: 13px;
}

.status-card strong {
  display: block;
  margin-top: 10px;
  font-size: 24px;
  color: #0f172a;
}

.status-tag,
.reopen-btn {
  margin-top: 14px;
}

.panel {
  margin-top: 22px;
  padding: 24px;
}

.panel-header {
  margin-bottom: 16px;
}

.panel-header p {
  margin: 10px 0 0;
  color: #475569;
  line-height: 1.8;
}

.answer-list {
  display: grid;
  gap: 16px;
}

.answer-card {
  position: relative;
  padding: 20px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  border-radius: 22px;
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.94), rgba(255, 255, 255, 0.98));
}

.answer-card.best {
  border-color: rgba(34, 197, 94, 0.35);
  box-shadow: inset 0 0 0 1px rgba(34, 197, 94, 0.2);
  background: linear-gradient(135deg, rgba(240, 253, 244, 0.94), rgba(255, 255, 255, 0.98));
}

.best-banner {
  position: absolute;
  top: 14px;
  right: 16px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #16a34a;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
}

.answer-body {
  color: #0f172a;
  line-height: 1.8;
}

.answer-meta {
  margin-top: 12px;
}

.answer-actions {
  margin-top: 14px;
}

.login-tip {
  margin-bottom: 12px;
}

.submit-bar {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

.empty-wrap {
  padding: 28px 0 8px;
}

@media (max-width: 820px) {
  .hero-head {
    grid-template-columns: 1fr;
  }
}
</style>

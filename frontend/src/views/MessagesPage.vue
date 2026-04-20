<template>
  <div class="message-page">
    <section class="message-shell">
      <aside class="conversation-panel">
        <div class="panel-head">
          <span class="hero-badge">站内私信</span>
          <h1>会话列表</h1>
          <p>查看与你的同学、作者或管理员之间的私信往来。</p>
        </div>

        <div class="conversation-list" v-loading="loadingConversations">
          <button
            v-for="item in conversations"
            :key="item.userId"
            :class="['conversation-item', { active: activeUserId === item.userId }]"
            @click="selectConversation(item.userId)"
          >
            <div class="conversation-top">
              <strong>{{ item.displayName }}</strong>
              <span>{{ formatTime(item.lastMessageAt) }}</span>
            </div>
            <p>{{ item.lastMessage || '点击进入会话后开始发送私信' }}</p>
            <div class="conversation-foot">
              <span>{{ item.username }}</span>
              <el-badge v-if="Number(item.unreadCount || 0) > 0" :value="item.unreadCount" />
            </div>
          </button>
        </div>
      </aside>

      <main class="chat-panel">
        <div v-if="activeConversation" class="chat-head">
          <h2>{{ activeConversation.displayName }}</h2>
          <p>{{ activeConversation.username }}</p>
          <small>最近同步：{{ lastSyncedAt || '等待同步' }}</small>
        </div>

        <div v-if="activeConversation" class="message-list" v-loading="loadingMessages">
          <article
            v-for="item in messages"
            :key="item.id"
            :class="['message-bubble', item.mine ? 'mine' : 'peer']"
          >
            <div class="bubble-name">{{ item.mine ? '我' : item.senderName }}</div>
            <div class="bubble-content">{{ item.content }}</div>
            <span class="bubble-time">{{ formatTime(item.createdAt) }}</span>
          </article>

          <div v-if="!loadingMessages && messages.length === 0" class="empty-wrap">
            <el-empty description="当前还没有私信记录，发一条消息试试吧。" />
          </div>
        </div>

        <div v-else class="chat-placeholder">
          <el-empty description="从左侧选择一个会话，或在用户主页中点击“发私信”开始聊天。" />
        </div>

        <div v-if="activeConversation" class="composer">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="4"
            resize="none"
            maxlength="1000"
            show-word-limit
            placeholder="输入想发送的私信内容"
          />
          <div class="composer-actions">
            <span>私信仅对会话双方可见，适合发送协作沟通和答疑内容。</span>
            <el-button type="primary" :loading="sending" @click="sendMessage">发送私信</el-button>
          </div>
        </div>
      </main>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import dayjs from 'dayjs';
import api from '../api';

const route = useRoute();
const router = useRouter();

const conversations = ref<any[]>([]);
const messages = ref<any[]>([]);
const loadingConversations = ref(false);
const loadingMessages = ref(false);
const sending = ref(false);
const draft = ref('');
const activeUserId = ref<number | null>(null);
const lastSyncedAt = ref('');
let refreshTimer: ReturnType<typeof window.setInterval> | undefined;

const activeConversation = computed(() => conversations.value.find((item) => item.userId === activeUserId.value) || null);

function formatTime(value?: string) {
  return value ? dayjs(value).format('MM-DD HH:mm') : '';
}

function syncRouteConversation() {
  const queryUserId = Number(route.query.userId || 0) || null;
  if (queryUserId) {
    activeUserId.value = queryUserId;
  } else if (!activeUserId.value && conversations.value.length) {
    activeUserId.value = conversations.value[0].userId;
  }
}

async function ensureConversationPlaceholder(userId: number) {
  if (!userId || conversations.value.some((item) => item.userId === userId)) {
    return;
  }
  try {
    const res = await api.get(`/profile/${userId}/space`);
    const user = res.data.data?.user;
    if (!user) {
      return;
    }
    conversations.value = [
      {
        userId,
        username: user.username,
        nickname: user.nickname,
        displayName: user.nickname || user.username || `用户 ${userId}`,
        lastMessage: '',
        lastMessageAt: null,
        unreadCount: 0
      },
      ...conversations.value
    ];
  } catch {
    // Ignore placeholder fetch failures.
  }
}

async function loadConversations() {
  loadingConversations.value = true;
  try {
    const res = await api.get('/message/conversations');
    conversations.value = res.data.data || [];
    await ensureConversationPlaceholder(Number(route.query.userId || 0) || 0);
    syncRouteConversation();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载私信会话失败');
  } finally {
    loadingConversations.value = false;
  }
}

async function refreshConversationState() {
  await loadConversations();
  if (activeUserId.value) {
    await loadMessages();
  }
  lastSyncedAt.value = dayjs().format('HH:mm:ss');
}

async function loadMessages() {
  if (!activeUserId.value) {
    messages.value = [];
    return;
  }
  loadingMessages.value = true;
  try {
    const res = await api.get(`/message/with/${activeUserId.value}`);
    messages.value = res.data.data || [];
    conversations.value = conversations.value.map((item) =>
      item.userId === activeUserId.value ? { ...item, unreadCount: 0 } : item
    );
    window.dispatchEvent(new Event('private-message-change'));
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载私信记录失败');
  } finally {
    loadingMessages.value = false;
  }
}

function selectConversation(userId: number) {
  router.replace({ path: '/messages', query: { userId: String(userId) } });
}

async function sendMessage() {
  const content = draft.value.trim();
  if (!activeUserId.value || !content) {
    ElMessage.warning('请输入私信内容后再发送');
    return;
  }
  sending.value = true;
  try {
    await api.post('/message', {
      receiverUserId: activeUserId.value,
      content
    });
    draft.value = '';
    await loadConversations();
    await loadMessages();
    ElMessage.success('私信发送成功');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '私信发送失败');
  } finally {
    sending.value = false;
  }
}

watch(
  () => route.query.userId,
  async () => {
    await ensureConversationPlaceholder(Number(route.query.userId || 0) || 0);
    syncRouteConversation();
    await loadMessages();
  }
);

onMounted(async () => {
  await refreshConversationState();
  window.addEventListener('private-message-change', refreshConversationState);
  window.addEventListener('focus', refreshConversationState);
  document.addEventListener('visibilitychange', refreshConversationState);
  refreshTimer = window.setInterval(() => {
    void refreshConversationState();
  }, 15000);
});

onBeforeUnmount(() => {
  window.removeEventListener('private-message-change', refreshConversationState);
  window.removeEventListener('focus', refreshConversationState);
  document.removeEventListener('visibilitychange', refreshConversationState);
  if (refreshTimer) {
    clearInterval(refreshTimer);
    refreshTimer = undefined;
  }
});
</script>

<style scoped>
.message-page { max-width: 1180px; margin: 0 auto; padding: 28px 20px 40px; }
.message-shell { display: grid; grid-template-columns: 320px minmax(0, 1fr); gap: 20px; min-height: 720px; }
.conversation-panel, .chat-panel { border-radius: 28px; background: rgba(255,255,255,.94); box-shadow: 0 20px 45px rgba(15,23,42,.08); }
.conversation-panel { padding: 24px; display: grid; grid-template-rows: auto 1fr; }
.hero-badge { display: inline-block; padding: 6px 12px; border-radius: 999px; background: rgba(249,115,22,.1); color: #c2410c; font-size: 12px; font-weight: 700; }
.panel-head h1, .chat-head h2 { margin: 14px 0 8px; color: #0f172a; }
.panel-head p, .chat-head p { margin: 0; color: #64748b; line-height: 1.7; }
.conversation-list { margin-top: 18px; display: grid; gap: 12px; align-content: start; overflow: auto; }
.conversation-item { border: 1px solid rgba(148,163,184,.18); border-radius: 20px; padding: 16px 18px; background: linear-gradient(180deg,#fff 0%,#fbfdff 100%); text-align: left; cursor: pointer; }
.conversation-item.active { border-color: rgba(37,99,235,.28); box-shadow: inset 0 0 0 1px rgba(37,99,235,.16); background: linear-gradient(180deg, rgba(239,246,255,.94) 0%, #fff 100%); }
.conversation-top, .conversation-foot { display: flex; align-items: center; justify-content: space-between; gap: 10px; }
.conversation-top strong { color: #0f172a; }
.conversation-top span, .conversation-foot span { color: #94a3b8; font-size: 12px; }
.conversation-item p { margin: 10px 0 12px; color: #475569; line-height: 1.6; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.chat-panel { display: grid; grid-template-rows: auto minmax(0,1fr) auto; overflow: hidden; }
.chat-head { padding: 24px 26px 18px; border-bottom: 1px solid #eef2f7; }
.chat-head small { display:block; margin-top:6px; color:#94a3b8; }
.message-list, .chat-placeholder { padding: 22px 26px; overflow: auto; display: grid; gap: 14px; align-content: start; }
.message-bubble { max-width: min(76%, 680px); padding: 14px 16px 12px; border-radius: 20px; display: grid; gap: 8px; }
.message-bubble.peer { background: #f6f8fc; border: 1px solid #e8edf5; }
.message-bubble.mine { justify-self: end; background: linear-gradient(135deg, #1677ff, #3b82f6); color: #fff; }
.bubble-name { font-size: 12px; font-weight: 700; opacity: .9; }
.bubble-content { line-height: 1.8; white-space: pre-wrap; word-break: break-word; }
.bubble-time { font-size: 12px; opacity: .78; }
.composer { padding: 18px 26px 24px; border-top: 1px solid #eef2f7; }
.composer-actions { margin-top: 12px; display: flex; align-items: center; justify-content: space-between; gap: 16px; color: #94a3b8; font-size: 13px; }
.empty-wrap { padding-top: 18px; }
@media (max-width: 920px) {
  .message-shell { grid-template-columns: 1fr; }
  .conversation-panel { max-height: 360px; }
  .composer-actions { display: grid; grid-template-columns: 1fr; }
}
</style>

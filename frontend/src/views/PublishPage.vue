<template>
  <div class="publish-wrap">
    <el-card class="publish-card" shadow="never">
      <div class="header-row">
        <div>
          <div class="page-tag">{{ isEditMode ? '内容编辑台' : '内容创作台' }}</div>
          <h2>{{ isEditMode ? '编辑内容' : '发布内容' }}</h2>
          <p>
            {{ isEditMode
              ? '对已发布或待审核内容进行修改后，系统将重新提交审核，审核通过后才会展示最新版本。'
              : '支持文章、教程与问答三类内容创作。正式发布的内容将统一进入审核流程，审核通过后才会对外展示。' }}
          </p>
        </div>
        <el-button plain @click="$router.push('/me')">返回我的</el-button>
      </div>

      <el-form label-position="top" class="publish-form">
        <el-row :gutter="16">
          <el-col :span="14">
            <el-form-item label="标题">
              <el-input v-model="form.title" placeholder="请输入清晰、准确的标题" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="内容类型">
              <el-select v-model="form.type" style="width: 100%">
                <el-option label="文章" value="ARTICLE" />
                <el-option label="教程" value="TUTORIAL" />
                <el-option label="问答" value="QUESTION" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" style="width: 100%" placeholder="请选择分类">
                <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="标签">
              <el-select
                v-model="form.tagIds"
                multiple
                collapse-tags
                collapse-tags-tooltip
                style="width: 100%"
                placeholder="请选择标签"
              >
                <el-option v-for="item in tags" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="可见范围">
              <el-select v-model="form.visibility" style="width: 100%">
                <el-option label="公开" value="PUBLIC" />
                <el-option label="仅粉丝可见" value="FOLLOWERS" />
                <el-option label="私密" value="PRIVATE" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="摘要">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请简要概括内容重点，便于审核和列表展示"
          />
        </el-form-item>

        <el-form-item label="正文">
          <div class="editor-shell">
            <div class="editor-toolbar">
              <div class="tool-group">
                <el-button size="small" plain @click="insertBlock('h2')">插入二级标题</el-button>
                <el-button size="small" plain @click="insertBlock('quote')">插入引用</el-button>
                <el-button size="small" plain @click="insertBlock('code')">插入代码块</el-button>
              </div>
              <div class="tool-group">
                <span class="tool-tip">支持上传图片，也可使用 Markdown 风格输入标题、引用和代码块</span>
                <input class="file-input" type="file" accept="image/*" @change="onImageSelected" />
              </div>
            </div>

            <div class="editor-grid">
              <div class="editor-pane">
                <div class="pane-title">编辑区</div>
                <el-input
                  v-model="form.body"
                  type="textarea"
                  :rows="18"
                  resize="vertical"
                  placeholder="请输入正文内容。建议使用分段、标题、引用和代码块提高可读性。"
                />
              </div>

              <div class="preview-pane">
                <div class="pane-title">预览效果</div>
                <div class="preview-body" v-html="previewHtml"></div>
              </div>
            </div>
          </div>
        </el-form-item>

        <div class="review-tip">
          <el-alert
            :title="isEditMode
              ? '修改说明：点击“保存修改”后，更新内容将重新进入审核队列；审核通过后才会替换线上展示版本。'
              : '发布说明：点击“发布”后，内容将进入审核队列；审核通过后才会在首页和详情页对外展示。'"
            type="info"
            :closable="false"
          />
        </div>

        <div class="action-row">
          <el-button :loading="submittingDraft" @click="submitDraft">保存草稿</el-button>
          <el-button type="primary" :loading="submittingPublish" @click="submitPublish">
            {{ isEditMode ? '保存修改' : '发布' }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '../api';

const route = useRoute();
const router = useRouter();
const contentId = computed(() => Number(route.query.id) || 0);
const isEditMode = computed(() => contentId.value > 0);

const categories = ref<any[]>([]);
const tags = ref<any[]>([]);
const submittingDraft = ref(false);
const submittingPublish = ref(false);

const form = reactive({
  type: 'ARTICLE',
  title: '',
  summary: '',
  body: '',
  categoryId: undefined as number | undefined,
  visibility: 'PUBLIC',
  tagIds: [] as number[]
});

const previewHtml = computed(() => {
  if (!form.body.trim()) {
    return '<p class="preview-placeholder">正文预览将显示在这里。你可以在左侧输入内容、插入标题、引用或代码块。</p>';
  }
  return form.body
    .replace(/\n```([\s\S]*?)```/g, '<pre><code>$1</code></pre>')
    .replace(/^##\s(.+)$/gm, '<h2>$1</h2>')
    .replace(/^>\s(.+)$/gm, '<blockquote>$1</blockquote>')
    .replace(/\n/g, '<br />');
});

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
  if (!form.categoryId && categories.value.length > 0) {
    form.categoryId = categories.value[0].id;
  }
  if (form.tagIds.length === 0 && tags.value.length > 0) {
    form.tagIds = [tags.value[0].id];
  }
}

async function loadContent() {
  if (!isEditMode.value) return;
  try {
    const res = await api.get(`/content/${contentId.value}`, { params: { incrementView: false } });
    const data = res.data.data;
    form.type = data.type || 'ARTICLE';
    form.title = data.title || '';
    form.summary = data.summary || '';
    form.body = data.body || '';
    form.categoryId = data.categoryId;
    form.visibility = data.visibility || 'PUBLIC';
    if (!form.tagIds.length) {
      form.tagIds = [];
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载待编辑内容失败');
  }
}

function insertBlock(type: 'h2' | 'quote' | 'code') {
  if (type === 'h2') {
    form.body += `${form.body ? '\n\n' : ''}## 小节标题\n`;
    return;
  }
  if (type === 'quote') {
    form.body += `${form.body ? '\n\n' : ''}> 在这里输入引用内容\n`;
    return;
  }
  form.body += `${form.body ? '\n\n' : ''}\`\`\`\n// 在这里输入代码示例\n\`\`\`\n`;
}

async function onImageSelected(e: Event) {
  const input = e.target as HTMLInputElement;
  if (!input.files || input.files.length === 0) return;
  const file = input.files[0];
  const fd = new FormData();
  fd.append('file', file);
  try {
    const res = await api.post('/uploads', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
    const url = res.data?.data;
    if (!url) throw new Error('missing upload url');
    form.body += `${form.body ? '\n\n' : ''}<img src="${url}" alt="upload" style="max-width:100%;border-radius:16px;" />\n`;
    ElMessage.success('图片上传成功');
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err.message || '图片上传失败');
  } finally {
    input.value = '';
  }
}

function validateForm() {
  if (!form.title.trim()) {
    ElMessage.warning('请输入标题');
    return false;
  }
  if (!form.categoryId) {
    ElMessage.warning('请选择分类');
    return false;
  }
  if (!form.body.trim()) {
    ElMessage.warning('请输入正文内容');
    return false;
  }
  return true;
}

function buildPayload(status?: string) {
  return {
    ...form,
    status,
    tagIds: form.tagIds
  };
}

async function submitDraft() {
  if (!validateForm()) return;
  submittingDraft.value = true;
  try {
    if (isEditMode.value) {
      await api.put(`/content/${contentId.value}`, buildPayload('DRAFT'));
    } else {
      await api.post('/content', buildPayload('DRAFT'));
    }
    ElMessage.success('草稿保存成功');
    router.push('/me');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '草稿保存失败');
  } finally {
    submittingDraft.value = false;
  }
}

async function submitPublish() {
  if (!validateForm()) return;
  submittingPublish.value = true;
  try {
    if (isEditMode.value) {
      await api.put(`/content/${contentId.value}`, buildPayload());
      ElMessage.success('修改已提交审核');
    } else {
      await api.post('/content', buildPayload());
      ElMessage.success('内容已提交审核');
    }
    router.push('/me');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '提交审核失败');
  } finally {
    submittingPublish.value = false;
  }
}

onMounted(async () => {
  await loadTaxonomy();
  await loadContent();
});
</script>

<style scoped>
.publish-wrap { max-width: 1160px; margin: 0 auto; }
.publish-card { border: none; border-radius: 30px; background: rgba(255,255,255,.92); box-shadow: 0 24px 60px rgba(15,23,42,.08); }
.header-row { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; margin-bottom:24px; }
.header-row h2 { margin:10px 0 8px; font-size:32px; color:#0f172a; }
.header-row p { margin:0; color:#64748b; line-height:1.75; max-width:760px; }
.page-tag { display:inline-block; padding:6px 12px; border-radius:999px; color:#1677ff; background:rgba(22,119,255,.12); font-size:12px; font-weight:700; }
.editor-shell { border:1px solid #dbe4f0; border-radius:24px; background:linear-gradient(180deg,#ffffff 0%,#f8fbff 100%); overflow:hidden; }
.editor-toolbar { display:flex; justify-content:space-between; align-items:center; gap:12px; padding:14px 16px; border-bottom:1px solid rgba(148,163,184,.22); background:rgba(248,250,252,.92); }
.tool-group { display:flex; align-items:center; gap:10px; flex-wrap:wrap; }
.tool-tip { color:#64748b; font-size:13px; }
.file-input { color:#475569; }
.editor-grid { display:grid; grid-template-columns:minmax(0,1fr) minmax(320px,.9fr); gap:0; }
.editor-pane, .preview-pane { padding:18px; }
.preview-pane { border-left:1px solid rgba(148,163,184,.22); background:rgba(248,250,252,.65); }
.pane-title { margin-bottom:12px; color:#475569; font-size:13px; font-weight:700; }
.preview-body { min-height:360px; padding:18px; border-radius:18px; background:#fff; color:#0f172a; line-height:1.85; box-shadow:inset 0 0 0 1px rgba(148,163,184,.16); }
.preview-body :deep(h2) { margin:0 0 14px; color:#0f172a; }
.preview-body :deep(blockquote) { margin:12px 0; padding:10px 14px; border-left:4px solid #1677ff; background:#f4f8ff; color:#475569; }
.preview-body :deep(pre) { overflow:auto; padding:14px; border-radius:14px; background:#0f172a; color:#e2e8f0; }
.preview-placeholder { color:#94a3b8; }
.review-tip { margin-top:8px; }
.action-row { display:flex; gap:12px; margin-top:14px; }
@media (max-width:980px) { .editor-grid { grid-template-columns:1fr; } .preview-pane { border-left:none; border-top:1px solid rgba(148,163,184,.22); } }
</style>

<template>
  <div class="publish-wrap">
    <el-card class="publish-card" shadow="never">
      <div class="header-row">
        <div>
          <div class="page-tag">{{ isEditMode ? '编辑内容' : '发布内容' }}</div>
          <h2>{{ isEditMode ? '编辑图文内容' : '创建图文内容' }}</h2>
          <p>
            支持图文混排、代码块、附件、封面图、拖拽上传和图片排序。
            这版编辑器重点补齐答辩展示最需要的内容组织能力，同时保留轻量、稳定的输入体验。
          </p>
        </div>
        <el-button plain @click="$router.push('/me')">返回我的内容</el-button>
      </div>

      <el-form label-position="top" class="publish-form">
        <el-row :gutter="16">
          <el-col :span="14">
            <el-form-item label="标题">
              <el-input v-model="form.title" placeholder="请输入文章、教程或问答标题" />
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
                <el-option label="仅自己可见" value="PRIVATE" />
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
            placeholder="摘要会显示在首页和内容列表中，建议用一两句话说明核心观点。"
          />
        </el-form-item>

        <el-form-item label="封面图片">
          <div class="cover-shell">
            <div class="cover-preview" :class="{ empty: !coverAsset }">
              <template v-if="coverAsset">
                <img :src="coverAsset.url" :alt="coverAsset.name" />
                <div class="cover-meta">
                  <strong>{{ coverAsset.name }}</strong>
                  <span>{{ formatAssetSize(coverAsset.size) }}</span>
                </div>
              </template>
              <template v-else>
                <strong>暂未设置封面</strong>
                <span>可上传一张主图，并在需要时插入正文顶部。</span>
              </template>
            </div>
            <div class="cover-actions">
              <label class="upload-button">
                <input type="file" accept="image/*" @change="onCoverSelected" />
                上传封面
              </label>
              <el-button plain :disabled="!coverAsset" @click="insertCoverBlock">按光标插入封面</el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="正文编辑器">
          <div
            class="editor-shell"
            :class="{ dragging: dragActive }"
            @dragover.prevent="onDragOver"
            @dragleave.prevent="onDragLeave"
            @drop.prevent="onDropFiles"
          >
            <div class="editor-toolbar">
              <div class="tool-block">
                <span class="tool-label">结构</span>
                <el-button size="small" plain @click="insertBlock('heading')">二级标题</el-button>
                <el-button size="small" plain @click="insertBlock('subheading')">三级标题</el-button>
                <el-button size="small" plain @click="insertBlock('divider')">分隔线</el-button>
                <el-button size="small" plain @click="insertBlock('quote')">引用</el-button>
                <el-button size="small" plain @click="insertBlock('list')">项目列表</el-button>
                <el-button size="small" plain @click="insertBlock('code')">代码块</el-button>
              </div>
              <div class="tool-block">
                <span class="tool-label">模板</span>
                <el-button size="small" plain @click="insertTemplate('article')">文章段落</el-button>
                <el-button size="small" plain @click="insertTemplate('tutorial')">教程步骤</el-button>
                <el-button size="small" plain @click="insertTemplate('qa')">问答结构</el-button>
                <el-button size="small" plain @click="insertTemplate('gallery')">双图展示</el-button>
              </div>
            </div>

            <div class="asset-toolbar">
              <div class="asset-toolbar-actions">
                <label class="upload-button">
                  <input type="file" accept="image/*" @change="onImageSelected" />
                  上传图片
                </label>
                <label class="upload-button">
                  <input
                    type="file"
                    accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.zip,.rar,image/*"
                    @change="onAttachmentSelected"
                  />
                  上传附件
                </label>
                <el-button
                  plain
                  :disabled="imageAssets.length === 0"
                  @click="insertImageGallery"
                >
                  按当前顺序插入图组
                </el-button>
              </div>
              <span class="tool-tip">支持直接把图片或附件拖进编辑器区域上传，并在光标位置插入。</span>
            </div>

            <div v-if="uploadedAssets.length > 0" class="asset-strip">
              <article
                v-for="(asset, index) in uploadedAssets"
                :key="asset.url"
                class="asset-card"
                :draggable="asset.kind === 'image'"
                @dragstart="onAssetDragStart(index)"
                @dragover.prevent="onAssetDragOver(index)"
                @drop.prevent="onAssetDrop(index)"
              >
                <div class="asset-card-head">
                  <span class="asset-kind">{{ asset.kind === 'image' ? '图片' : '附件' }}</span>
                  <span class="asset-size">{{ formatAssetSize(asset.size) }}</span>
                </div>
                <strong>{{ asset.name }}</strong>
                <small>{{ asset.contentType || '未知类型' }}</small>
                <div class="asset-card-actions">
                  <el-button size="small" plain @click="insertAsset(asset)">按光标插入</el-button>
                  <template v-if="asset.kind === 'image'">
                    <el-button size="small" plain @click="moveAsset(index, -1)" :disabled="!canMoveAsset(index, -1)">上移</el-button>
                    <el-button size="small" plain @click="moveAsset(index, 1)" :disabled="!canMoveAsset(index, 1)">下移</el-button>
                  </template>
                  <el-button size="small" plain type="danger" @click="removeAsset(asset.url)">移除</el-button>
                </div>
              </article>
            </div>

            <div class="editor-grid">
              <div class="editor-pane">
                <div class="pane-title">
                  <span>编辑区</span>
                  <span>{{ bodyLength }} 字符</span>
                </div>
                <textarea
                  ref="editorRef"
                  v-model="form.body"
                  class="editor-textarea"
                  placeholder="在这里输入正文。上传图片、附件或插入模板时，会按照当前光标位置写入内容。"
                  @click="captureCursor"
                  @keyup="captureCursor"
                  @select="captureCursor"
                />
              </div>

              <div class="preview-pane">
                <div class="pane-title">
                  <span>预览区</span>
                  <span>实时渲染</span>
                </div>
                <div class="preview-body" v-html="previewHtml"></div>
              </div>
            </div>
          </div>
        </el-form-item>

        <div class="review-tip">
          <el-alert
            :title="isEditMode
              ? '修改后会重新进入审核流程，请确认图片顺序、附件链接和正文结构都已整理完成。'
              : '提交后内容会先进入审核列表，通过后才会在公开页面展示。'"
            type="info"
            :closable="false"
          />
        </div>

        <div class="action-row">
          <el-button :loading="submittingDraft" @click="submitDraft">保存草稿</el-button>
          <el-button type="primary" :loading="submittingPublish" @click="submitPublish">
            {{ isEditMode ? '保存并重新提交' : '提交审核' }}
          </el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import api from '../api';

type AssetKind = 'image' | 'file';

type UploadedAsset = {
  name: string;
  url: string;
  kind: AssetKind;
  size: number;
  contentType: string;
};

const route = useRoute();
const router = useRouter();
const editorRef = ref<HTMLTextAreaElement | null>(null);
const contentId = computed(() => Number(route.query.id) || 0);
const isEditMode = computed(() => contentId.value > 0);

const categories = ref<Array<{ id: number; name: string }>>([]);
const tags = ref<Array<{ id: number; name: string }>>([]);
const submittingDraft = ref(false);
const submittingPublish = ref(false);
const uploadedAssets = ref<UploadedAsset[]>([]);
const coverAsset = ref<UploadedAsset | null>(null);
const dragActive = ref(false);
const dragAssetIndex = ref<number | null>(null);
const cursorRange = ref({ start: 0, end: 0 });

const form = reactive({
  type: 'ARTICLE',
  title: '',
  summary: '',
  body: '',
  categoryId: undefined as number | undefined,
  visibility: 'PUBLIC',
  tagIds: [] as number[]
});

const bodyLength = computed(() => form.body.trim().length);
const imageAssets = computed(() => uploadedAssets.value.filter((item) => item.kind === 'image'));
const previewHtml = computed(() => renderPreview(form.body));

function renderPreview(source: string) {
  if (!source.trim()) {
    return '<p class="preview-placeholder">正文预览会显示在这里。你可以拖入图片或附件，也可以使用上方模板快速组织图文结构。</p>';
  }

  let html = source
    .replace(/\r\n/g, '\n')
    .replace(/```([\s\S]*?)```/g, (_match, code) => `<pre><code>${escapeHtml(code.trim())}</code></pre>`)
    .replace(/^###\s(.+)$/gm, '<h3>$1</h3>')
    .replace(/^##\s(.+)$/gm, '<h2>$1</h2>')
    .replace(/^>\s(.+)$/gm, '<blockquote>$1</blockquote>')
    .replace(/^- (.+)$/gm, '<li>$1</li>')
    .replace(/^\*\*\*(.*)$/gm, '<hr />')
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n{2,}/g, '</p><p>');

  html = html.replace(/(<li>.*<\/li>)/gs, '<ul>$1</ul>');
  html = `<p>${html}</p>`;
  html = html.replace(/<p>\s*(<h2|<h3|<blockquote|<pre|<figure|<div|<ul|<hr)/g, '$1');
  html = html.replace(/(<\/h2>|<\/h3>|<\/blockquote>|<\/pre>|<\/figure>|<\/div>|<\/ul>|<hr \/>)\s*<\/p>/g, '$1');
  html = html.replace(/<p>\s*<\/p>/g, '');
  return html;
}

function escapeHtml(value: string) {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function sanitizeTaxonomyName(value: string) {
  return (value || '').trim() || '未命名';
}

function formatAssetSize(size?: number) {
  if (!size) return '未知大小';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / 1024 / 1024).toFixed(1)} MB`;
}

function captureCursor() {
  if (!editorRef.value) return;
  cursorRange.value = {
    start: editorRef.value.selectionStart || 0,
    end: editorRef.value.selectionEnd || 0
  };
}

async function focusEditor(start?: number, end?: number) {
  await nextTick();
  if (!editorRef.value) return;
  const nextStart = typeof start === 'number' ? start : cursorRange.value.start;
  const nextEnd = typeof end === 'number' ? end : nextStart;
  editorRef.value.focus();
  editorRef.value.setSelectionRange(nextStart, nextEnd);
  cursorRange.value = { start: nextStart, end: nextEnd };
}

function insertAtCursor(block: string) {
  const text = form.body || '';
  const { start, end } = cursorRange.value;
  const before = text.slice(0, start);
  const after = text.slice(end);
  const prefix = before && !before.endsWith('\n') ? '\n\n' : '';
  const suffix = after && !after.startsWith('\n') ? '\n\n' : '';
  const next = `${before}${prefix}${block}${suffix}${after}`;
  const nextCursor = (before + prefix + block).length;
  form.body = next;
  focusEditor(nextCursor, nextCursor);
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

function inferAssetsFromBody() {
  const assets: UploadedAsset[] = [];
  const matched = new Set<string>();

  const figureRegex = /<img[^>]+src="([^"]+)"[^>]*alt="([^"]*)"[^>]*>/g;
  let imageMatch: RegExpExecArray | null;
  while ((imageMatch = figureRegex.exec(form.body)) !== null) {
    const url = imageMatch[1];
    if (matched.has(url)) continue;
    matched.add(url);
    assets.push({
      name: imageMatch[2] || url.split('/').pop() || '图片资源',
      url,
      kind: 'image',
      size: 0,
      contentType: ''
    });
  }

  const attachmentRegex = /<div class="attachment-card">[\s\S]*?<strong>([^<]+)<\/strong>[\s\S]*?<a href="([^"]+)"/g;
  let attachmentMatch: RegExpExecArray | null;
  while ((attachmentMatch = attachmentRegex.exec(form.body)) !== null) {
    const url = attachmentMatch[2];
    if (matched.has(url)) continue;
    matched.add(url);
    assets.push({
      name: attachmentMatch[1] || url.split('/').pop() || '附件资源',
      url,
      kind: 'file',
      size: 0,
      contentType: ''
    });
  }

  uploadedAssets.value = assets;
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
    form.tagIds = Array.isArray(data.tagIds) ? data.tagIds : [];
    inferAssetsFromBody();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载待编辑内容失败');
  }
}

function insertBlock(type: 'heading' | 'subheading' | 'divider' | 'quote' | 'list' | 'code') {
  if (type === 'heading') {
    insertAtCursor('## 小节标题');
    return;
  }
  if (type === 'subheading') {
    insertAtCursor('### 细分主题');
    return;
  }
  if (type === 'divider') {
    insertAtCursor('***');
    return;
  }
  if (type === 'quote') {
    insertAtCursor('> 在这里输入引用、摘要或关键观点。');
    return;
  }
  if (type === 'list') {
    insertAtCursor('- 要点一\n- 要点二\n- 要点三');
    return;
  }
  insertAtCursor('```\n// 在这里输入代码示例\n```');
}

function insertTemplate(type: 'article' | 'tutorial' | 'qa' | 'gallery') {
  if (type === 'article') {
    insertAtCursor([
      '## 核心观点',
      '这里先用一段文字概括本节重点。',
      '',
      '## 展开说明',
      '再用两到三段正文详细说明背景、做法和结果。'
    ].join('\n'));
    return;
  }

  if (type === 'tutorial') {
    insertAtCursor([
      '## 操作步骤',
      '- 第一步：说明准备工作',
      '- 第二步：说明关键配置',
      '- 第三步：说明运行结果',
      '',
      '> 小提示：可以在步骤之间插入截图或附件链接。'
    ].join('\n'));
    return;
  }

  if (type === 'qa') {
    insertAtCursor([
      '## 问题背景',
      '说明问题发生在什么场景下。',
      '',
      '## 排查过程',
      '- 观察到的现象',
      '- 尝试过的办法',
      '- 最终定位结果',
      '',
      '## 解决方案',
      '给出可以直接复用的解决办法。'
    ].join('\n'));
    return;
  }

  insertAtCursor([
    '<div class="image-gallery">',
    '  <figure class="content-figure">',
    '    <img src="https://placehold.co/640x360?text=Image+1" alt="示意图一" />',
    '    <figcaption>图一说明</figcaption>',
    '  </figure>',
    '  <figure class="content-figure">',
    '    <img src="https://placehold.co/640x360?text=Image+2" alt="示意图二" />',
    '    <figcaption>图二说明</figcaption>',
    '  </figure>',
    '</div>'
  ].join('\n'));
}

function toAsset(payload: any, fallbackFile?: File, kindOverride?: AssetKind): UploadedAsset {
  const url = typeof payload === 'string' ? payload : payload?.url;
  if (!url) {
    throw new Error('missing upload url');
  }

  const inferredKind = kindOverride || (payload?.kind === 'image' ? 'image' : 'file');
  return {
    name: payload?.name || fallbackFile?.name || '未命名文件',
    url,
    kind: inferredKind,
    size: Number(payload?.size || fallbackFile?.size || 0),
    contentType: payload?.contentType || fallbackFile?.type || ''
  };
}

function buildImageBlock(asset: UploadedAsset) {
  return [
    '<figure class="content-figure">',
    `  <img src="${asset.url}" alt="${asset.name}" />`,
    `  <figcaption>${asset.name}</figcaption>`,
    '</figure>'
  ].join('\n');
}

function buildAttachmentBlock(asset: UploadedAsset) {
  return [
    '<div class="attachment-card">',
    '  <div class="attachment-card__icon">附件</div>',
    '  <div class="attachment-card__body">',
    `    <strong>${asset.name}</strong>`,
    `    <span>${formatAssetSize(asset.size)}${asset.contentType ? ` · ${asset.contentType}` : ''}</span>`,
    `    <a href="${asset.url}" target="_blank" rel="noreferrer noopener">点击下载或预览</a>`,
    '  </div>',
    '</div>'
  ].join('\n');
}

function insertCoverBlock() {
  if (!coverAsset.value) return;
  insertAtCursor(buildImageBlock(coverAsset.value));
}

function normalizeAssetOrder() {
  uploadedAssets.value = [...uploadedAssets.value];
}

function canMoveAsset(index: number, delta: number) {
  const target = index + delta;
  if (target < 0 || target >= uploadedAssets.value.length) {
    return false;
  }
  return uploadedAssets.value[index]?.kind === 'image' && uploadedAssets.value[target]?.kind === 'image';
}

function moveAsset(index: number, delta: number) {
  if (!canMoveAsset(index, delta)) return;
  const next = [...uploadedAssets.value];
  const target = index + delta;
  [next[index], next[target]] = [next[target], next[index]];
  uploadedAssets.value = next;
}

function onAssetDragStart(index: number) {
  dragAssetIndex.value = index;
}

function onAssetDragOver(index: number) {
  if (dragAssetIndex.value === null || dragAssetIndex.value === index) return;
  if (uploadedAssets.value[dragAssetIndex.value]?.kind !== 'image' || uploadedAssets.value[index]?.kind !== 'image') return;
}

function onAssetDrop(index: number) {
  if (dragAssetIndex.value === null || dragAssetIndex.value === index) return;
  const from = dragAssetIndex.value;
  if (uploadedAssets.value[from]?.kind !== 'image' || uploadedAssets.value[index]?.kind !== 'image') {
    dragAssetIndex.value = null;
    return;
  }
  const next = [...uploadedAssets.value];
  const [moved] = next.splice(from, 1);
  next.splice(index, 0, moved);
  uploadedAssets.value = next;
  dragAssetIndex.value = null;
}

function insertImageGallery() {
  if (imageAssets.value.length === 0) return;
  const gallery = [
    '<div class="image-gallery">',
    ...imageAssets.value.map((asset) => [
      '  <figure class="content-figure">',
      `    <img src="${asset.url}" alt="${asset.name}" />`,
      `    <figcaption>${asset.name}</figcaption>`,
      '  </figure>'
    ].join('\n')),
    '</div>'
  ].join('\n');
  insertAtCursor(gallery);
}

async function onCoverSelected(e: Event) {
  const input = e.target as HTMLInputElement;
  if (!input.files || input.files.length === 0) return;
  try {
    const asset = await uploadAsset(input.files[0], 'image');
    coverAsset.value = asset;
    ElMessage.success('封面上传成功');
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err.message || '封面上传失败');
  } finally {
    input.value = '';
  }
}

async function onImageSelected(e: Event) {
  const input = e.target as HTMLInputElement;
  if (!input.files || input.files.length === 0) return;
  try {
    const asset = await uploadAsset(input.files[0], 'image');
    insertAtCursor(buildImageBlock(asset));
    ElMessage.success('图片已按光标位置插入');
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err.message || '图片上传失败');
  } finally {
    input.value = '';
  }
}

async function onAttachmentSelected(e: Event) {
  const input = e.target as HTMLInputElement;
  if (!input.files || input.files.length === 0) return;
  try {
    const asset = await uploadAsset(input.files[0], 'file');
    insertAtCursor(buildAttachmentBlock(asset));
    ElMessage.success('附件已按光标位置插入');
  } catch (err: any) {
    ElMessage.error(err?.response?.data?.message || err.message || '附件上传失败');
  } finally {
    input.value = '';
  }
}

async function uploadAsset(file: File, kind: AssetKind) {
  const fd = new FormData();
  fd.append('file', file);
  const res = await api.post('/uploads', fd, { headers: { 'Content-Type': 'multipart/form-data' } });
  const asset = toAsset(res.data?.data, file, kind);
  uploadedAssets.value = [
    ...uploadedAssets.value.filter((item) => item.url !== asset.url),
    asset
  ];
  normalizeAssetOrder();
  return asset;
}

function insertAsset(asset: UploadedAsset) {
  insertAtCursor(asset.kind === 'image' ? buildImageBlock(asset) : buildAttachmentBlock(asset));
}

function removeAsset(url: string) {
  uploadedAssets.value = uploadedAssets.value.filter((asset) => asset.url !== url);
  if (coverAsset.value?.url === url) {
    coverAsset.value = null;
  }
}

function onDragOver() {
  dragActive.value = true;
}

function onDragLeave() {
  dragActive.value = false;
}

async function onDropFiles(e: DragEvent) {
  dragActive.value = false;
  const files = Array.from(e.dataTransfer?.files || []);
  if (files.length === 0) return;

  for (const file of files) {
    try {
      const kind: AssetKind = file.type.startsWith('image/') ? 'image' : 'file';
      const asset = await uploadAsset(file, kind);
      insertAtCursor(kind === 'image' ? buildImageBlock(asset) : buildAttachmentBlock(asset));
    } catch (err: any) {
      ElMessage.error(err?.response?.data?.message || err.message || `上传失败：${file.name}`);
    }
  }

  ElMessage.success(`已上传 ${files.length} 个文件`);
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
      ElMessage.success('内容修改成功，已重新提交审核');
    } else {
      await api.post('/content', buildPayload());
      ElMessage.success('内容提交成功，已进入审核队列');
    }
    router.push('/me');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '提交失败');
  } finally {
    submittingPublish.value = false;
  }
}

onMounted(async () => {
  await loadTaxonomy();
  await loadContent();
  await focusEditor(0, 0);
});
</script>

<style scoped>
.publish-wrap {
  max-width: 1180px;
  margin: 0 auto;
}

.publish-card {
  border: none;
  border-radius: 30px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
}

.header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 24px;
}

.header-row h2 {
  margin: 10px 0 8px;
  font-size: 34px;
  color: #0f172a;
}

.header-row p {
  margin: 0;
  color: #64748b;
  line-height: 1.8;
  max-width: 760px;
}

.page-tag {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  color: #1677ff;
  background: rgba(22, 119, 255, 0.12);
  font-size: 12px;
  font-weight: 700;
}

.cover-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 16px;
  align-items: stretch;
}

.cover-preview {
  display: grid;
  gap: 12px;
  min-height: 180px;
  padding: 16px;
  border: 1px solid #dbe4f0;
  border-radius: 24px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
}

.cover-preview.empty {
  place-items: center;
  text-align: center;
  color: #64748b;
}

.cover-preview img {
  width: 100%;
  max-height: 260px;
  object-fit: cover;
  border-radius: 18px;
}

.cover-meta {
  display: grid;
  gap: 4px;
  color: #475569;
}

.cover-actions {
  display: grid;
  gap: 10px;
  align-content: start;
}

.upload-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 120px;
  height: 40px;
  padding: 0 14px;
  border-radius: 12px;
  background: #1677ff;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 600;
}

.upload-button input {
  display: none;
}

.editor-shell {
  border: 1px solid #dbe4f0;
  border-radius: 24px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  overflow: hidden;
  transition: box-shadow 0.2s ease, border-color 0.2s ease;
}

.editor-shell.dragging {
  border-color: #1677ff;
  box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.12);
}

.editor-toolbar,
.asset-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
}

.editor-toolbar {
  border-bottom: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(248, 250, 252, 0.94);
}

.asset-toolbar {
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
  background: rgba(255, 255, 255, 0.88);
}

.asset-toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tool-block {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.tool-label {
  margin-right: 4px;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.tool-tip {
  color: #64748b;
  font-size: 13px;
}

.asset-strip {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.18);
}

.asset-card {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 18px;
  background: #fff;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.18);
}

.asset-card-head,
.asset-card-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.asset-kind {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: #e8f2ff;
  color: #1677ff;
  font-size: 12px;
  font-weight: 700;
}

.asset-size,
.asset-card small {
  color: #64748b;
  font-size: 12px;
}

.editor-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.95fr);
}

.editor-pane,
.preview-pane {
  padding: 18px;
}

.preview-pane {
  border-left: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(248, 250, 252, 0.68);
}

.pane-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
}

.editor-textarea {
  width: 100%;
  min-height: 520px;
  padding: 16px 18px;
  border: 1px solid #dbe4f0;
  border-radius: 18px;
  background: #fff;
  color: #0f172a;
  font: inherit;
  line-height: 1.9;
  resize: vertical;
  outline: none;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.08);
}

.editor-textarea:focus {
  border-color: #1677ff;
  box-shadow: 0 0 0 4px rgba(22, 119, 255, 0.1);
}

.preview-body {
  min-height: 520px;
  padding: 18px;
  border-radius: 18px;
  background: #fff;
  color: #0f172a;
  line-height: 1.9;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.16);
}

.preview-body :deep(p) {
  margin: 0 0 16px;
}

.preview-body :deep(h2) {
  margin: 24px 0 12px;
  font-size: 26px;
  color: #0f172a;
}

.preview-body :deep(h3) {
  margin: 20px 0 10px;
  font-size: 20px;
  color: #1e293b;
}

.preview-body :deep(blockquote) {
  margin: 18px 0;
  padding: 12px 16px;
  border-left: 4px solid #1677ff;
  border-radius: 0 14px 14px 0;
  background: #f5f8ff;
  color: #475569;
}

.preview-body :deep(pre) {
  overflow: auto;
  margin: 20px 0;
  padding: 16px;
  border-radius: 14px;
  background: #0f172a;
  color: #e2e8f0;
}

.preview-body :deep(ul) {
  margin: 0 0 18px 20px;
  padding: 0;
}

.preview-body :deep(li) {
  margin-bottom: 8px;
}

.preview-body :deep(hr) {
  margin: 24px 0;
  border: none;
  border-top: 1px solid #dbe4f0;
}

.preview-body :deep(.content-figure) {
  display: grid;
  gap: 10px;
  margin: 20px 0;
}

.preview-body :deep(.content-figure img) {
  width: 100%;
  border-radius: 18px;
  box-shadow: 0 14px 24px rgba(15, 23, 42, 0.1);
}

.preview-body :deep(.content-figure figcaption) {
  color: #64748b;
  font-size: 13px;
  text-align: center;
}

.preview-body :deep(.image-gallery) {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.preview-body :deep(.attachment-card) {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  margin: 20px 0;
  padding: 16px;
  border-radius: 18px;
  background: #f8fbff;
  box-shadow: inset 0 0 0 1px rgba(148, 163, 184, 0.18);
}

.preview-body :deep(.attachment-card__icon) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: linear-gradient(135deg, #1677ff, #59a0ff);
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.preview-body :deep(.attachment-card__body) {
  display: grid;
  gap: 4px;
}

.preview-body :deep(.attachment-card__body strong) {
  color: #0f172a;
}

.preview-body :deep(.attachment-card__body span),
.preview-body :deep(.attachment-card__body a) {
  color: #64748b;
  font-size: 13px;
}

.preview-placeholder {
  color: #94a3b8;
}

.review-tip {
  margin-top: 8px;
}

.action-row {
  display: flex;
  gap: 12px;
  margin-top: 14px;
}

@media (max-width: 980px) {
  .cover-shell,
  .editor-grid {
    grid-template-columns: 1fr;
  }

  .preview-pane {
    border-left: none;
    border-top: 1px solid rgba(148, 163, 184, 0.22);
  }

  .header-row,
  .editor-toolbar,
  .asset-toolbar {
    display: grid;
  }

  .preview-body :deep(.image-gallery) {
    grid-template-columns: 1fr;
  }
}
</style>

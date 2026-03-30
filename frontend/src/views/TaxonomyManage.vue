<template>
  <div class="page-shell">
    <section class="hero-card">
      <div>
        <p class="eyebrow">后台维护</p>
        <h1>分类与标签管理</h1>
        <p class="hero-desc">用于维护发布页下拉数据，支持新增分类和标签，便于论文演示后台管理能力。</p>
      </div>
    </section>

    <div class="grid">
      <section class="panel">
        <div class="panel-header">
          <div>
            <h2>新增分类</h2>
            <p>创建后会直接出现在首页筛选和发布页下拉中。</p>
          </div>
        </div>
        <el-form label-position="top">
          <el-form-item label="分类名称">
            <el-input v-model="categoryForm.name" placeholder="例如：系统设计" />
          </el-form-item>
          <el-form-item label="父级分类">
            <el-select v-model="categoryForm.parentId" placeholder="顶级分类">
              <el-option :value="0" label="顶级分类" />
              <el-option v-for="item in categories" :key="item.id" :label="item.name" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="排序值">
            <el-input-number v-model="categoryForm.sort" :min="0" />
          </el-form-item>
          <el-button type="primary" @click="submitCategory">新增分类</el-button>
        </el-form>
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h2>新增标签</h2>
            <p>标签会用于发布绑定、推荐召回和搜索筛选。</p>
          </div>
        </div>
        <el-form label-position="top">
          <el-form-item label="标签名称">
            <el-input v-model="tagForm.name" placeholder="例如：架构设计" />
          </el-form-item>
          <el-button type="primary" @click="submitTag">新增标签</el-button>
        </el-form>
      </section>
    </div>

    <div class="grid">
      <section class="panel">
        <div class="panel-header">
          <div>
            <h2>当前分类</h2>
          </div>
        </div>
        <div class="chips">
          <span v-for="item in categories" :key="item.id" class="chip">{{ item.name }}</span>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <h2>当前标签</h2>
          </div>
        </div>
        <div class="chips">
          <span v-for="item in tags" :key="item.id" class="chip">{{ item.name }}</span>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import api from '../api';

const categories = ref<any[]>([]);
const tags = ref<any[]>([]);

const categoryForm = reactive({
  name: '',
  parentId: 0,
  sort: 0
});

const tagForm = reactive({
  name: ''
});

async function load() {
  const [categoryRes, tagRes] = await Promise.all([
    api.get('/public/taxonomy/categories'),
    api.get('/public/taxonomy/tags')
  ]);
  categories.value = categoryRes.data.data || [];
  tags.value = tagRes.data.data || [];
}

async function submitCategory() {
  if (!categoryForm.name.trim()) {
    ElMessage.warning('请输入分类名称');
    return;
  }
  try {
    await api.post('/admin/taxonomy/categories', categoryForm);
    categoryForm.name = '';
    categoryForm.parentId = 0;
    categoryForm.sort = 0;
    await load();
    ElMessage.success('分类新增成功');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '分类新增失败');
  }
}

async function submitTag() {
  if (!tagForm.name.trim()) {
    ElMessage.warning('请输入标签名称');
    return;
  }
  try {
    await api.post('/admin/taxonomy/tags', tagForm);
    tagForm.name = '';
    await load();
    ElMessage.success('标签新增成功');
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '标签新增失败');
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
.panel {
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 28px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.08);
}

.hero-card {
  padding: 28px 32px;
  background:
    radial-gradient(circle at top right, rgba(20, 184, 166, 0.18), transparent 36%),
    linear-gradient(135deg, rgba(240, 253, 250, 0.98), rgba(255, 255, 255, 0.92));
}

.eyebrow {
  margin: 0 0 10px;
  color: #0f766e;
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
  line-height: 1.7;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
  margin-top: 22px;
}

.panel {
  padding: 24px;
}

.panel-header {
  margin-bottom: 16px;
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.chip {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  background: rgba(15, 118, 110, 0.08);
  color: #0f766e;
  font-weight: 600;
}

@media (max-width: 840px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>

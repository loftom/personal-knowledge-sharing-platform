<template>
  <div class="sensitive-word-page">
    <div class="page-header">
      <div>
        <h2>敏感词管理</h2>
        <p>管理平台内容审核使用的敏感词列表，支持增删改和启用/禁用操作。</p>
      </div>
      <el-button type="primary" @click="openAdd">添加敏感词</el-button>
    </div>

    <div class="word-table-wrap">
      <el-table :data="words" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="word" label="敏感词" min-width="180" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'danger' : 'info'">
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" plain @click="openEdit(row)">编辑</el-button>
            <el-button size="small" plain :type="row.enabled === 1 ? 'warning' : 'success'"
              @click="toggleEnabled(row)">
              {{ row.enabled === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-popconfirm title="确定删除该敏感词？" @confirm="removeWord(row.id)">
              <template #reference>
                <el-button size="small" plain type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="isEditing ? '编辑敏感词' : '添加敏感词'" width="480px">
      <el-form :model="form" label-position="top">
        <el-form-item label="敏感词">
          <el-input v-model="form.word" placeholder="请输入敏感词" maxlength="64" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabledFlag" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import api from '../api';

const words = ref<any[]>([]);
const loading = ref(false);
const dialogVisible = ref(false);
const isEditing = ref(false);
const editingId = ref<number | null>(null);
const submitting = ref(false);

const form = reactive({
  word: '',
  enabledFlag: true
});

async function loadWords() {
  loading.value = true;
  try {
    const res = await api.get('/admin/sensitive-word');
    words.value = res.data.data || [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载敏感词列表失败');
  } finally {
    loading.value = false;
  }
}

function openAdd() {
  isEditing.value = false;
  editingId.value = null;
  form.word = '';
  form.enabledFlag = true;
  dialogVisible.value = true;
}

function openEdit(row: any) {
  isEditing.value = true;
  editingId.value = row.id;
  form.word = row.word;
  form.enabledFlag = row.enabled === 1;
  dialogVisible.value = true;
}

async function submit() {
  if (!form.word.trim()) {
    ElMessage.warning('请输入敏感词');
    return;
  }
  submitting.value = true;
  try {
    if (isEditing.value && editingId.value) {
      await api.put(`/admin/sensitive-word/${editingId.value}`, {
        word: form.word.trim(),
        enabled: form.enabledFlag ? 1 : 0
      });
      ElMessage.success('敏感词已更新');
    } else {
      await api.post('/admin/sensitive-word', { word: form.word.trim() });
      ElMessage.success('敏感词已添加');
    }
    dialogVisible.value = false;
    await loadWords();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '操作失败');
  } finally {
    submitting.value = false;
  }
}

async function toggleEnabled(row: any) {
  try {
    await api.put(`/admin/sensitive-word/${row.id}`, {
      word: row.word,
      enabled: row.enabled === 1 ? 0 : 1
    });
    ElMessage.success(row.enabled === 1 ? '已禁用' : '已启用');
    await loadWords();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '操作失败');
  }
}

async function removeWord(id: number) {
  try {
    await api.delete(`/admin/sensitive-word/${id}`);
    ElMessage.success('敏感词已删除');
    await loadWords();
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '删除失败');
  }
}

onMounted(loadWords);
</script>

<style scoped>
.sensitive-word-page {
  display: grid;
  gap: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.page-header h2 {
  margin: 0 0 8px;
  color: #0f172a;
}

.page-header p {
  margin: 0;
  color: #64748b;
  line-height: 1.75;
}

.word-table-wrap {
  border-radius: 16px;
  overflow: hidden;
  background: #fff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}
</style>

<template>
  <div class="admin-preference-page">
    <div class="page-head">
      <div>
        <div class="section-tag">用户偏好</div>
        <h2>兴趣画像概览</h2>
        <p>管理员可在此查看各用户基于点赞、收藏、评论等行为累积出的标签偏好，用于解释当前个性化推荐结果。</p>
      </div>
      <el-button @click="load">刷新数据</el-button>
    </div>

    <el-card class="panel-card" shadow="never">
      <el-table :data="list" :loading="loading">
        <el-table-column label="用户" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <strong>{{ row.displayName }}</strong>
              <small>{{ row.username }}</small>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'info'">{{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="偏好标签数" width="120">
          <template #default="{ row }">{{ row.tagCount || 0 }}</template>
        </el-table-column>
        <el-table-column label="偏好强度" width="120">
          <template #default="{ row }">{{ formatWeight(row.totalWeight) }}</template>
        </el-table-column>
        <el-table-column label="主要偏好标签" min-width="360">
          <template #default="{ row }">
            <div v-if="row.preferences?.length" class="tag-wrap">
              <el-tag
                v-for="tag in row.preferences"
                :key="`${row.userId}-${tag.tagId}`"
                class="pref-tag"
                effect="plain"
                round
              >
                {{ tag.tagName }} · {{ formatWeight(tag.weight) }}
              </el-tag>
            </div>
            <span v-else class="empty-text">暂无明显偏好</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && !list.length" class="empty-wrap">
        <el-empty description="当前暂无用户偏好数据。" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import api from '../api';

const list = ref<any[]>([]);
const loading = ref(false);

function formatWeight(value?: number) {
  return Number(value || 0).toFixed(1);
}

async function load() {
  loading.value = true;
  try {
    const res = await api.get('/admin/audit/preferences');
    list.value = res.data.data || [];
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '加载用户偏好失败');
  } finally {
    loading.value = false;
  }
}

onMounted(load);
</script>

<style scoped>
.admin-preference-page {
  display: grid;
  gap: 18px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
}

.page-head h2 {
  margin: 8px 0 6px;
  font-size: 30px;
}

.page-head p {
  margin: 0;
  color: #64748b;
  line-height: 1.75;
}

.section-tag {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  color: #1d4ed8;
  background: rgba(37, 99, 235, 0.12);
  font-size: 12px;
  font-weight: 700;
}

.panel-card {
  border: none;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 24px 50px rgba(15, 23, 42, 0.08);
}

.user-cell {
  display: grid;
  gap: 4px;
}

.user-cell strong {
  color: #0f172a;
}

.user-cell small,
.empty-text {
  color: #64748b;
}

.tag-wrap {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pref-tag {
  margin: 2px 0;
}

.empty-wrap {
  margin-top: 18px;
}
</style>

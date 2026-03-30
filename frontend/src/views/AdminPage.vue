<template>
  <div class="admin-page">
    <section class="admin-hero">
      <div>
        <div class="section-tag">管理员后台</div>
        <h1>后台管理中心</h1>
        <p>管理员功能独立于普通用户前台，集中承接内容审核、分类标签维护与用户兴趣画像查看。</p>
      </div>
    </section>

    <el-alert
      v-if="!isAdmin"
      title="当前账号不是管理员，无法访问后台管理功能。"
      type="warning"
      :closable="false"
    />

    <el-tabs v-else v-model="activeTab" class="admin-tabs">
      <el-tab-pane label="审核台" name="audit">
        <AdminAuditPage />
      </el-tab-pane>
      <el-tab-pane label="用户偏好" name="preference">
        <AdminPreferencePage />
      </el-tab-pane>
      <el-tab-pane label="分类标签" name="taxonomy">
        <TaxonomyManage />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import TaxonomyManage from './TaxonomyManage.vue';
import AdminAuditPage from './AdminAuditPage.vue';
import AdminPreferencePage from './AdminPreferencePage.vue';

const isAdmin = computed(() => localStorage.getItem('role') === 'ADMIN');
const activeTab = ref('audit');
</script>

<style scoped>
.admin-page {
  display: grid;
  gap: 20px;
}

.admin-hero {
  padding: 28px;
  border-radius: 30px;
  color: #fff;
  background: linear-gradient(135deg, #0f172a 0%, #1d4ed8 46%, #0f766e 100%);
}

.admin-hero h1 {
  margin: 12px 0 8px;
  font-size: 36px;
}

.admin-hero p {
  margin: 0;
  color: rgba(255, 255, 255, 0.86);
}

.section-tag {
  display: inline-block;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 12px;
  font-weight: 700;
}
</style>

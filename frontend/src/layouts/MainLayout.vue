<template>
  <el-container class="main-layout">
    <el-aside :width="collapsed ? '64px' : '200px'" class="aside">
      <div class="logo">
        <h2 v-if="!collapsed">贸易数据系统</h2>
        <h2 v-else>贸</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="collapsed"
        :unique-opened="true"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><data-analysis /></el-icon>
          <template #title>首页看板</template>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/trade">
          <el-icon><document /></el-icon>
          <template #title>贸易数据管理</template>
        </el-menu-item>
        <el-menu-item v-if="isAdmin" index="/news">
          <el-icon><reading /></el-icon>
          <template #title>新闻数据管理</template>
        </el-menu-item>
        <el-menu-item index="/stats">
          <el-icon><trend-charts /></el-icon>
          <template #title>统计分析</template>
        </el-menu-item>
        <el-menu-item index="/ai">
          <el-icon><chat-dot-round /></el-icon>
          <template #title>AI智能问答</template>
        </el-menu-item>
        <el-sub-menu index="/knowledge-graph">
          <template #title>
            <el-icon><share /></el-icon>
            <span>知识图谱</span>
          </template>
          <el-menu-item index="/knowledge-graph/history">
            <template #title>历史列表</template>
          </el-menu-item>
          <el-menu-item index="/knowledge-graph/create">
            <template #title>创建/生成</template>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-icon" @click="toggleCollapsed">
            <fold v-if="!collapsed" />
            <expand v-else />
          </el-icon>
        </div>
        <div class="header-right">
          <span class="user-info">欢迎，{{ username }}</span>
          <el-button type="text" @click="handleLogout">
            <el-icon><switch-button /></el-icon>
            退出
          </el-button>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useAppStore } from '@/stores/app'
import {
  DataAnalysis,
  Document,
  Reading,
  TrendCharts,
  ChatDotRound,
  Share,
  Fold,
  Expand,
  SwitchButton
} from '@element-plus/icons-vue'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

const collapsed = computed(() => appStore.collapsed)
const isAdmin = computed(() => userStore.isAdmin)
const username = computed(() => userStore.username)
const activeMenu = computed(() => route.path)

const toggleCollapsed = () => {
  appStore.toggleCollapsed()
}

const handleLogout = () => {
  ElMessageBox.confirm('确定要退出登录吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    userStore.logout()
    router.push('/login')
  }).catch(() => {})
}
</script>

<style scoped>
.main-layout {
  height: 100%;
}

.aside {
  background-color: #304156;
  transition: width 0.3s;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background-color: #2b3a4a;
}

.logo h2 {
  margin: 0;
  font-size: 18px;
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}

.collapse-icon {
  font-size: 20px;
  cursor: pointer;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.user-info {
  color: #606266;
}

.main {
  background-color: #f0f2f5;
  padding: 20px;
}
</style>

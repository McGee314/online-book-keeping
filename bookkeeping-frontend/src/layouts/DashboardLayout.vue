<script setup>
import { ref, computed, provide } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()
const isCollapse = ref(false)

const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

// Provide a callback for child views to register their "open add transaction" handler
const addTransactionCallback = ref(null)
provide('onAddTransaction', (fn) => {
  addTransactionCallback.value = fn
})

function handleAddTransaction() {
  addTransactionCallback.value?.()
}

const menuItems = [
  { path: '/dashboard', title: 'Dashboard', icon: 'DataAnalysis' },
  { path: '/transactions', title: 'Transactions', icon: 'Money' },
  { path: '/categories', title: 'Categories', icon: 'Collection' },
]

function isActive(path) {
  return route.path.startsWith(path)
}

function navigate(path) {
  router.push(path)
}

function handleLogout() {
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  router.push('/login')
}
</script>

<template>
  <div class="flex h-screen bg-background">
    <!-- Sidebar -->
    <aside
      class="bg-primary text-white flex flex-col transition-all duration-300"
      :class="isCollapse ? 'w-16' : 'w-60'"
    >
      <!-- Logo -->
      <div class="flex items-center h-16 px-4 border-b border-primary-dark">
        <span v-if="!isCollapse" class="text-lg font-semibold tracking-wide">BookKeeper</span>
        <span v-else class="text-lg font-semibold">BK</span>
      </div>

      <!-- Menu -->
      <nav class="flex-1 py-4">
        <div
          v-for="item in menuItems"
          :key="item.path"
          @click="navigate(item.path)"
          class="flex items-center mx-2 mb-1 px-3 py-3 rounded-lg cursor-pointer transition-colors duration-200"
          :class="isActive(item.path) ? 'bg-primary-dark text-white' : 'text-white/80 hover:bg-primary-dark hover:text-white'"
        >
          <div class="w-6 h-6 flex items-center justify-center text-lg">
            <span v-if="item.icon === 'DataAnalysis'">📊</span>
            <span v-else-if="item.icon === 'Money'">💰</span>
            <span v-else-if="item.icon === 'Collection'">📂</span>
          </div>
          <span v-if="!isCollapse" class="ml-3 text-sm font-medium">{{ item.title }}</span>
        </div>
      </nav>

      <!-- Collapse toggle -->
      <div
        @click="isCollapse = !isCollapse"
        class="flex items-center justify-center h-12 mx-2 mb-4 rounded-lg cursor-pointer bg-primary-dark hover:bg-primary-light transition-colors"
      >
        <span class="text-white text-lg">{{ isCollapse ? '☰' : '✕' }}</span>
      </div>
    </aside>

    <!-- Main Content -->
    <div class="flex-1 flex flex-col overflow-hidden">
      <!-- Header -->
      <header class="bg-surface border-b border-gray-200 h-16 flex items-center justify-between px-6 flex-shrink-0">
        <div>
          <h1 class="text-lg font-semibold text-text-primary">{{ route.meta.title }}</h1>
        </div>
        <div class="flex items-center gap-4">
          <el-button type="primary" size="small" @click="handleAddTransaction" plain>
            ＋ Add Transaction
          </el-button>
          <span class="text-sm text-text-regular">{{ userInfo.nickname || userInfo.username }}</span>
          <el-button type="danger" size="small" @click="handleLogout" plain>Logout</el-button>
        </div>
      </header>

      <!-- Page Content -->
      <main class="flex-1 overflow-auto p-6">
        <router-view />
      </main>
    </div>
  </div>
</template>
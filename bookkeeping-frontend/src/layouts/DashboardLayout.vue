<script setup>
import { ref, computed, provide, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()

const sidebarOpen = ref(true)
const mobileMenuOpen = ref(false)
const isMobile = ref(false)

const userInfo = computed(() => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
})

const displayName = computed(() => userInfo.value.nickname || userInfo.value.username || 'User')
const userInitial = computed(() => (displayName.value || 'U').charAt(0).toUpperCase())
const avatarUrl = computed(() => {
  const avatar = userInfo.value.avatar
  if (!avatar) return ''
  if (avatar.startsWith('/uploads/') || avatar.startsWith('http')) return avatar
  return ''
})

const addTransactionCallback = ref(null)
provide('onAddTransaction', (fn) => {
  addTransactionCallback.value = fn
})

const displayCurrency = ref('CNY')
provide('displayCurrency', displayCurrency)

const menuItems = [
  { path: '/dashboard', title: 'Home', icon: 'home' },
  { path: '/transactions', title: 'Transactions', icon: 'transactions' },
  { path: '/categories', title: 'Categories', icon: 'categories' },
  { path: '/profile', title: 'Account Settings', icon: 'profile' },
]

function isActive(path) {
  return route.path.startsWith(path)
}

function navigate(path) {
  router.push(path)
  if (isMobile.value) mobileMenuOpen.value = false
}

function handleAddTransaction() {
  addTransactionCallback.value?.()
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm(
      'Are you sure you want to sign out?',
      'Sign Out',
      { confirmButtonText: 'Yes, Sign Out', cancelButtonText: 'Cancel', type: 'warning' }
    )
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    router.push('/login')
  } catch {
    // User cancelled
  }
}

function checkMobile() {
  isMobile.value = window.innerWidth < 768
  if (isMobile.value) sidebarOpen.value = false
  else sidebarOpen.value = true
}

onMounted(() => {
  checkMobile()
  window.addEventListener('resize', checkMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkMobile)
})
</script>

<template>
  <div class="flex h-screen overflow-hidden bg-background">
    <!-- Mobile Overlay -->
    <div
      v-if="isMobile && mobileMenuOpen"
      class="fixed inset-0 bg-black/40 z-40 transition-opacity duration-300"
      @click="mobileMenuOpen = false"
    ></div>

    <!-- Sidebar -->
    <aside
      class="flex flex-col bg-white border-r border-border-light z-50 transition-all duration-300 ease-in-out shrink-0"
      :class="[
        isMobile
          ? `fixed inset-y-0 left-0 ${mobileMenuOpen ? 'translate-x-0' : '-translate-x-full'} w-64`
          : `relative ${sidebarOpen ? 'w-64' : 'w-[72px]'}`,
      ]"
    >
      <!-- Logo -->
      <div class="flex items-center h-16 px-5 border-b border-border-light shrink-0">
        <div class="flex items-center gap-3 overflow-hidden">
          <div class="w-9 h-9 rounded-xl bg-primary-600 text-white flex items-center justify-center shrink-0 shadow-sm shadow-primary-600/25">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
          </div>
          <span
            class="font-extrabold text-lg text-text-primary whitespace-nowrap transition-opacity duration-200"
            :class="{ 'opacity-0 w-0': !sidebarOpen && !isMobile }"
          >isCash</span>
        </div>
      </div>

      <!-- Navigation -->
      <nav class="flex-1 py-4 px-3 space-y-1 overflow-y-auto">
        <p
          class="px-3 mb-2 text-2xs font-semibold uppercase tracking-widest text-text-muted whitespace-nowrap transition-opacity duration-200"
          :class="{ 'opacity-0': !sidebarOpen && !isMobile }"
        >Menu</p>
        <button
          v-for="item in menuItems"
          :key="item.path"
          @click="navigate(item.path)"
          class="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 group text-left border-none cursor-pointer"
          :class="isActive(item.path)
            ? 'bg-primary-50 text-primary-700 shadow-sm'
            : 'text-text-secondary hover:bg-surface-secondary hover:text-text-primary'"
          :title="!sidebarOpen && !isMobile ? item.title : ''"
        >
          <!-- Home icon -->
          <svg v-if="item.icon === 'home'" class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6" />
          </svg>
          <!-- Transactions icon -->
          <svg v-else-if="item.icon === 'transactions'" class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01" />
          </svg>
          <!-- Categories icon -->
          <svg v-else-if="item.icon === 'categories'" class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
          </svg>
          <!-- Profile icon -->
          <svg v-else-if="item.icon === 'profile'" class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.066 2.573c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.573 1.066c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.066-2.573c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
          <span
            class="whitespace-nowrap transition-opacity duration-200"
            :class="{ 'opacity-0 w-0 overflow-hidden': !sidebarOpen && !isMobile }"
          >{{ item.title }}</span>
        </button>
      </nav>

      <!-- Bottom: User + Collapse -->
      <div class="border-t border-border-light p-3 space-y-2 shrink-0">
        <div
          class="flex items-center gap-3 px-2 py-2 rounded-xl transition-colors overflow-hidden"
          :class="{ 'justify-center': !sidebarOpen && !isMobile }"
        >
          <div class="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center font-bold text-sm shrink-0 overflow-hidden">
            <img v-if="avatarUrl" :src="avatarUrl" class="w-full h-full object-cover" alt="" />
            <span v-else>{{ userInitial }}</span>
          </div>
          <div
            class="overflow-hidden transition-opacity duration-200"
            :class="{ 'opacity-0 w-0': !sidebarOpen && !isMobile }"
          >
            <p class="text-sm font-semibold text-text-primary truncate">{{ displayName }}</p>
            <p class="text-2xs text-text-muted">My Account</p>
          </div>
        </div>

        <button
          v-if="!isMobile"
          @click="sidebarOpen = !sidebarOpen"
          class="w-full flex items-center justify-center gap-2 py-2 rounded-lg text-text-muted hover:bg-surface-secondary hover:text-text-secondary transition-all text-sm border-none bg-transparent cursor-pointer"
          :title="sidebarOpen ? 'Collapse sidebar' : 'Expand sidebar'"
        >
          <svg class="w-4 h-4 transition-transform duration-300" :class="{ 'rotate-180': sidebarOpen }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 19l-7-7 7-7m8 14l-7-7 7-7" />
          </svg>
        </button>
      </div>
    </aside>

    <!-- Main Content Area -->
    <div class="flex-1 flex flex-col min-w-0 overflow-hidden">
      <!-- Header -->
      <header class="bg-white border-b border-border-light h-16 flex items-center justify-between px-4 sm:px-6 shrink-0 shadow-navbar">
        <div class="flex items-center gap-3 min-w-0">
          <button
            v-if="isMobile"
            @click="mobileMenuOpen = !mobileMenuOpen"
            class="text-text-primary border-none bg-transparent cursor-pointer p-1"
            aria-label="Toggle menu"
          >
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <div class="min-w-0">
            <h1 class="text-base sm:text-lg font-bold text-text-primary truncate">
              {{ route.meta?.title || 'Dashboard' }}
            </h1>
          </div>
        </div>

        <!-- Right Actions -->
        <div class="flex items-center gap-2 sm:gap-3 shrink-0">
          <div class="flex items-center gap-2 bg-surface-secondary rounded-lg px-3 py-1.5">
            <span class="text-2xs font-semibold text-text-muted uppercase tracking-wider hidden sm:inline">Show in</span>
            <el-select
              v-model="displayCurrency"
              size="small"
              class="currency-select"
              popper-class="currency-popper"
            >
              <el-option label="CNY" value="CNY" />
              <el-option label="USD" value="USD" />
              <el-option label="IDR" value="IDR" />
              <el-option label="SGD" value="SGD" />
              <el-option label="AUD" value="AUD" />
              <el-option label="EUR" value="EUR" />
              <el-option label="GBP" value="GBP" />
              <el-option label="JPY" value="JPY" />
              <el-option label="MAD" value="MAD" />
              <el-option label="RUB" value="RUB" />
            </el-select>
          </div>

          <el-button
            type="primary"
            size="small"
            @click="handleAddTransaction"
            class="font-semibold !rounded-xl"
            v-if="route.path.startsWith('/dashboard') || route.path.startsWith('/transactions')"
          >
            <span class="hidden sm:inline">+ Add Transaction</span>
            <span class="sm:hidden">+</span>
          </el-button>

          <el-button
            size="small"
            @click="handleLogout"
            class="font-semibold !rounded-xl"
            plain
          >
            <span class="hidden sm:inline">Sign Out</span>
            <span class="sm:hidden">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" />
              </svg>
            </span>
          </el-button>
        </div>
      </header>

      <!-- Page Content -->
      <main class="flex-1 overflow-y-auto p-4 sm:p-6 bg-background">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
.currency-select {
  width: 100px;
}
.currency-select :deep(.el-input__wrapper) {
  background: transparent;
  box-shadow: none;
  padding: 0 4px;
}
.currency-select :deep(.el-input__inner) {
  font-weight: 600;
  font-size: 0.8125rem;
}
</style>
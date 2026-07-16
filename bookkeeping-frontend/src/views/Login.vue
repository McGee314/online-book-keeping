<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const showPassword = ref(false)
const errorMessage = ref('')

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [
    { required: true, message: 'Please enter your username', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Please enter your password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' },
  ],
}

const formRef = ref(null)

async function handleLogin() {
  errorMessage.value = ''
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const data = await login({ username: form.username, password: form.password })
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
    ElMessage.success('Welcome back! You have logged in successfully.')
    router.push('/dashboard')
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Login failed. Please check your username and password.'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center p-4 sm:p-6 bg-gradient-to-br from-slate-50 via-green-50/30 to-blue-50/40">
    <!-- Background decoration -->
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="absolute -top-40 -right-40 w-[600px] h-[600px] bg-primary-100/30 rounded-full blur-3xl"></div>
      <div class="absolute -bottom-40 -left-40 w-[600px] h-[600px] bg-accent-100/30 rounded-full blur-3xl"></div>
    </div>

    <!-- Card -->
    <div class="relative w-full max-w-md animate-fade-in">
      <!-- Logo area -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-primary-600 text-white shadow-lg shadow-primary-600/25 mb-5">
          <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h1 class="text-2xl font-extrabold text-text-primary tracking-tight">Welcome Back</h1>
        <p class="text-text-secondary mt-1.5 text-sm">Sign in to continue tracking your finances</p>
      </div>

      <!-- Form Card -->
      <div class="bg-white rounded-2xl shadow-xl shadow-slate-200/50 p-8">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleLogin"
          class="space-y-1"
        >
          <el-form-item label="Username" prop="username">
            <el-input
              v-model="form.username"
              placeholder="Enter your username"
              size="large"
            >
              <template #prefix>
                <svg class="w-4 h-4 text-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="Password" prop="password">
            <el-input
              v-model="form.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="Enter your password"
              size="large"
            >
              <template #prefix>
                <svg class="w-4 h-4 text-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
              </template>
              <template #suffix>
                <button
                  type="button"
                  @click="showPassword = !showPassword"
                  class="text-text-muted hover:text-text-secondary transition-colors bg-transparent border-none cursor-pointer p-0 leading-none"
                  :aria-label="showPassword ? 'Hide password' : 'Show password'"
                >
                  <svg v-if="!showPassword" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
                  </svg>
                  <svg v-else class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.269-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.243 4.243M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21" />
                  </svg>
                </button>
              </template>
            </el-input>
          </el-form-item>

          <div class="pt-2">
            <el-button
              type="primary"
              size="large"
              class="w-full"
              :loading="loading"
              @click="handleLogin"
            >
              Sign In
            </el-button>
          </div>
        </el-form>

        <!-- Divider -->
        <div class="relative my-6">
          <div class="absolute inset-0 flex items-center">
            <div class="w-full border-t border-border-light"></div>
          </div>
          <div class="relative flex justify-center text-sm">
            <span class="px-3 bg-white text-text-muted">New to isCash?</span>
          </div>
        </div>

        <!-- Register link -->
        <router-link
          to="/register"
          class="flex items-center justify-center gap-2 w-full py-3 rounded-xl border-2 border-border-default text-text-secondary font-semibold text-sm hover:border-primary-300 hover:text-primary-600 transition-all duration-200 no-underline"
        >
          Create a new account
        </router-link>
      </div>

      <!-- Footer -->
      <p class="text-center text-xs text-text-muted mt-6">
        Securely track your daily income & expenses
      </p>
    </div>
  </div>
</template>
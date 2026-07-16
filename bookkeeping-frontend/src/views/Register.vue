<script setup>
import { reactive, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)
const showPassword = ref(false)
const showConfirm = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
})

const rules = {
  username: [
    { required: true, message: 'Please choose a username', trigger: 'blur' },
    { min: 3, message: 'Username must be at least 3 characters', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Please create a password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm your password', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        if (value !== form.password) {
          callback(new Error('Passwords do not match'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  nickname: [
    { required: true, message: 'Please enter your display name', trigger: 'blur' },
  ],
}

const formRef = ref(null)

const passwordStrength = computed(() => {
  const p = form.password || ''
  if (p.length === 0) return { level: 0, text: '', color: '' }
  if (p.length < 6) return { level: 1, text: 'Too short', color: 'bg-rose-400' }
  if (p.length < 8) return { level: 2, text: 'Medium', color: 'bg-warm-400' }
  const hasUpper = /[A-Z]/.test(p)
  const hasNumber = /[0-9]/.test(p)
  const hasSpecial = /[!@#$%^&*(),.?":{}|<>]/.test(p)
  const score = (hasUpper ? 1 : 0) + (hasNumber ? 1 : 0) + (hasSpecial ? 1 : 0)
  if (score >= 2 && p.length >= 8) return { level: 4, text: 'Strong', color: 'bg-primary-500' }
  return { level: 3, text: 'Good', color: 'bg-primary-400' }
})

async function handleRegister() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const data = await register({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
    })
    localStorage.setItem('token', data.token)
    localStorage.setItem('userInfo', JSON.stringify(data.userInfo))
    ElMessage.success('Account created! Welcome to isCash.')
    router.push('/dashboard')
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Registration failed. Please try again.'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center p-4 sm:p-6 bg-gradient-to-br from-slate-50 via-blue-50/30 to-green-50/40">
    <div class="absolute inset-0 overflow-hidden pointer-events-none">
      <div class="absolute -top-40 -left-40 w-[600px] h-[600px] bg-accent-100/30 rounded-full blur-3xl"></div>
      <div class="absolute -bottom-40 -right-40 w-[600px] h-[600px] bg-primary-100/30 rounded-full blur-3xl"></div>
    </div>

    <div class="relative w-full max-w-md animate-fade-in">
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-2xl bg-accent-600 text-white shadow-lg shadow-accent-600/25 mb-5">
          <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
          </svg>
        </div>
        <h1 class="text-2xl font-extrabold text-text-primary tracking-tight">Create Your Account</h1>
        <p class="text-text-secondary mt-1.5 text-sm">Start managing your money in minutes</p>
      </div>

      <div class="bg-white rounded-2xl shadow-xl shadow-slate-200/50 p-8">
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent="handleRegister"
          class="space-y-1"
        >
          <el-form-item label="Display Name" prop="nickname">
            <el-input
              v-model="form.nickname"
              placeholder="What should we call you?"
              size="large"
            >
              <template #prefix>
                <svg class="w-4 h-4 text-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5.121 17.804A13.937 13.937 0 0112 16c2.5 0 4.847.655 6.879 1.804M15 10a3 3 0 11-6 0 3 3 0 016 0zm6 2a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item label="Username" prop="username">
            <el-input
              v-model="form.username"
              placeholder="Pick a username for login"
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
              placeholder="Create a strong password"
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
            <div v-if="form.password" class="mt-2">
              <div class="flex gap-1 mb-1">
                <div
                  v-for="i in 4"
                  :key="i"
                  class="h-1 flex-1 rounded-full transition-all duration-300"
                  :class="i <= passwordStrength.level ? passwordStrength.color : 'bg-gray-200'"
                ></div>
              </div>
              <span class="text-2xs text-text-muted">{{ passwordStrength.text }}</span>
            </div>
          </el-form-item>

          <el-form-item label="Confirm Password" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              :type="showConfirm ? 'text' : 'password'"
              placeholder="Re-enter your password"
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
                  @click="showConfirm = !showConfirm"
                  class="text-text-muted hover:text-text-secondary transition-colors bg-transparent border-none cursor-pointer p-0 leading-none"
                  :aria-label="showConfirm ? 'Hide password' : 'Show password'"
                >
                  <svg v-if="!showConfirm" class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
              @click="handleRegister"
            >
              Create My Account
            </el-button>
          </div>
        </el-form>

        <div class="relative my-6">
          <div class="absolute inset-0 flex items-center">
            <div class="w-full border-t border-border-light"></div>
          </div>
          <div class="relative flex justify-center text-sm">
            <span class="px-3 bg-white text-text-muted">Already have an account?</span>
          </div>
        </div>

        <router-link
          to="/login"
          class="flex items-center justify-center gap-2 w-full py-3 rounded-xl border-2 border-border-default text-text-secondary font-semibold text-sm hover:border-primary-300 hover:text-primary-600 transition-all duration-200 no-underline"
        >
          Sign in instead
        </router-link>
      </div>

      <p class="text-center text-xs text-text-muted mt-6">
        Your data is kept private and secure
      </p>
    </div>
  </div>
</template>
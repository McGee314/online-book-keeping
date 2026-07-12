<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '../api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
})

const rules = {
  username: [
    { required: true, message: 'Please enter username', trigger: 'blur' },
    { min: 3, message: 'Username must be at least 3 characters', trigger: 'blur' },
  ],
  password: [
    { required: true, message: 'Please enter password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm password', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
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
    { required: true, message: 'Please enter nickname', trigger: 'blur' },
  ],
}

const formRef = ref(null)

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
    ElMessage.success('Registration successful!')
    router.push('/dashboard')
  } catch {
    // Error already handled by interceptor
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen bg-background flex items-center justify-center p-4">
    <div class="bg-surface rounded-xl shadow-lg p-8 w-full max-w-md">
      <!-- Title -->
      <div class="text-center mb-8">
        <h1 class="text-2xl font-bold text-primary mb-1">Create Account</h1>
        <p class="text-text-secondary text-sm">Start managing your finances</p>
      </div>

      <!-- Form -->
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent="handleRegister">
        <el-form-item label="Username" prop="username">
          <el-input v-model="form.username" placeholder="Choose a username" size="large" />
        </el-form-item>

        <el-form-item label="Nickname" prop="nickname">
          <el-input v-model="form.nickname" placeholder="Your display name" size="large" />
        </el-form-item>

        <el-form-item label="Password" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="Create a password"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item label="Confirm Password" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            placeholder="Re-enter your password"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            class="w-full"
            :loading="loading"
            @click="handleRegister"
          >
            Register
          </el-button>
        </el-form-item>
      </el-form>

      <!-- Login link -->
      <div class="text-center mt-4">
        <span class="text-text-secondary text-sm">Already have an account? </span>
        <router-link to="/login" class="text-primary font-medium text-sm hover:underline">
          Login
        </router-link>
      </div>
    </div>
  </div>
</template>
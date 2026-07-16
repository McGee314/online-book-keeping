<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getProfile, updateProfile, uploadAvatar, changePassword } from '../api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()

// --- Profile Info ---
const profileFormRef = ref(null)
const profileLoading = ref(false)
const initialProfileLoading = ref(true)
const avatarUploading = ref(false)
const fileInputRef = ref(null)

const profileForm = reactive({
  nickname: '',
  email: '',
})

const profileRules = {
  nickname: [
    { max: 100, message: 'Nickname must be at most 100 characters', trigger: 'blur' },
  ],
  email: [
    { type: 'email', message: 'Please enter a valid email address', trigger: 'blur' },
    { max: 100, message: 'Email must be at most 100 characters', trigger: 'blur' },
  ],
}

const userDisplay = reactive({
  username: '',
  id: null,
  avatar: '',
})

// --- Change Password ---
const passwordFormRef = ref(null)
const passwordLoading = ref(false)

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const passwordRules = {
  oldPassword: [
    { required: true, message: 'Please enter your current password', trigger: 'blur' },
  ],
  newPassword: [
    { required: true, message: 'Please enter a new password', trigger: 'blur' },
    { min: 6, message: 'Password must be at least 6 characters', trigger: 'blur' },
    { max: 100, message: 'Password must be at most 100 characters', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: 'Please confirm your new password', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== passwordForm.newPassword) {
          callback(new Error('Passwords do not match'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

// --- Avatar helpers ---
function getAvatarUrl() {
  if (!userDisplay.avatar) return ''
  // If it's a relative path like /uploads/..., prepend base.
  if (userDisplay.avatar.startsWith('/uploads/')) {
    return userDisplay.avatar
  }
  // If already a full URL
  if (userDisplay.avatar.startsWith('http')) {
    return userDisplay.avatar
  }
  return userDisplay.avatar
}

function triggerFileInput() {
  fileInputRef.value?.click()
}

function handleFileSelect(event) {
  const file = event.target.files?.[0]
  if (file) {
    doUpload(file)
  }
  // Reset so the same file can be re-selected
  event.target.value = ''
}

function handleDrop(event) {
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    doUpload(file)
  }
}

function handleDragOver(event) {
  event.preventDefault()
}

async function doUpload(file) {
  // Validate client-side
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('Only JPEG, PNG, GIF, and WebP images are allowed')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.error('File size must be less than 2MB')
    return
  }

  avatarUploading.value = true
  try {
    const data = await uploadAvatar(file)
    userDisplay.avatar = data.avatar
    // Update localStorage
    const stored = JSON.parse(localStorage.getItem('userInfo') || '{}')
    stored.avatar = data.avatar
    localStorage.setItem('userInfo', JSON.stringify(stored))
    ElMessage.success('Avatar updated successfully')
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Failed to upload avatar'
    ElMessage.error(msg)
  } finally {
    avatarUploading.value = false
  }
}

// --- Load Profile ---
async function loadProfile() {
  initialProfileLoading.value = true
  try {
    const data = await getProfile()
    userDisplay.username = data.username
    userDisplay.id = data.id
    userDisplay.avatar = data.avatar || ''
    profileForm.nickname = data.nickname || ''
    profileForm.email = data.email || ''
  } catch {
    ElMessage.error('Failed to load profile')
  } finally {
    initialProfileLoading.value = false
  }
}

// --- Submit Profile Update ---
async function handleProfileUpdate() {
  if (!profileFormRef.value) return
  try {
    await profileFormRef.value.validate()
  } catch {
    return
  }

  profileLoading.value = true
  try {
    const updated = await updateProfile({
      nickname: profileForm.nickname || null,
      email: profileForm.email || null,
    })
    // Update localStorage
    localStorage.setItem('userInfo', JSON.stringify(updated))
    ElMessage.success('Profile updated successfully')
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Failed to update profile'
    ElMessage.error(msg)
  } finally {
    profileLoading.value = false
  }
}

// --- Submit Password Change ---
async function handlePasswordChange() {
  if (!passwordFormRef.value) return
  try {
    await passwordFormRef.value.validate()
  } catch {
    return
  }

  passwordLoading.value = true
  try {
    await changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      confirmPassword: passwordForm.confirmPassword,
    })
    ElMessage.success('Password changed successfully')
    // Clear form
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordFormRef.value.resetFields()
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Failed to change password'
    ElMessage.error(msg)
  } finally {
    passwordLoading.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="max-w-2xl mx-auto space-y-6 pb-10">
    <h2 class="text-xl font-bold text-text-primary">Account Settings</h2>

    <!-- Loading skeleton -->
    <div v-if="initialProfileLoading" class="flex items-center justify-center py-20">
      <div class="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
    </div>

    <template v-else>
      <!-- Profile Info Card -->
      <div class="bg-white rounded-2xl shadow-sm border border-border-light p-6 sm:p-8">
        <!-- Avatar section -->
        <div class="flex flex-col items-center mb-8">
          <div
            class="relative w-24 h-24 rounded-full overflow-hidden mb-4 group cursor-pointer"
            @click="triggerFileInput"
            @dragover="handleDragOver"
            @drop.prevent="handleDrop"
          >
            <!-- Avatar image or fallback -->
            <img
              v-if="userDisplay.avatar"
              :src="getAvatarUrl()"
              class="w-full h-full object-cover"
              :alt="userDisplay.username"
            />
            <div
              v-else
              class="w-full h-full bg-primary-100 text-primary-700 flex items-center justify-center font-bold text-3xl"
            >
              {{ (userDisplay.username || 'U').charAt(0).toUpperCase() }}
            </div>
            <!-- Hover overlay -->
            <div
              class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center"
              :class="{ 'opacity-100': avatarUploading }"
            >
              <svg
                v-if="!avatarUploading"
                class="w-6 h-6 text-white"
                fill="none"
                stroke="currentColor"
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z" />
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 13a3 3 0 11-6 0 3 3 0 016 0z" />
              </svg>
              <div v-else class="animate-spin rounded-full h-6 w-6 border-2 border-white border-t-transparent"></div>
            </div>
          </div>
          <p class="text-sm text-text-muted mb-2">Click or drag & drop to change avatar</p>
          <p class="text-xs text-text-muted">JPEG, PNG, GIF, WebP • Max 2MB</p>
          <!-- Hidden file input -->
          <input
            ref="fileInputRef"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            class="hidden"
            @change="handleFileSelect"
          />
        </div>

        <div class="flex items-center gap-4 mb-6">
          <div>
            <h3 class="text-lg font-semibold text-text-primary">{{ userDisplay.username }}</h3>
            <p class="text-sm text-text-muted">Edit your personal information</p>
          </div>
        </div>

        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-position="top"
          @submit.prevent="handleProfileUpdate"
          class="space-y-1"
        >
          <el-form-item label="Nickname" prop="nickname">
            <el-input
              v-model="profileForm.nickname"
              placeholder="Enter your display name"
              size="large"
            />
          </el-form-item>

          <el-form-item label="Email" prop="email">
            <el-input
              v-model="profileForm.email"
              placeholder="Enter your email address"
              size="large"
            />
          </el-form-item>

          <div class="pt-2">
            <el-button
              type="primary"
              size="large"
              :loading="profileLoading"
              @click="handleProfileUpdate"
            >
              Save Changes
            </el-button>
          </div>
        </el-form>
      </div>

      <!-- Change Password Card -->
      <div class="bg-white rounded-2xl shadow-sm border border-border-light p-6 sm:p-8">
        <div class="mb-6">
          <h3 class="text-lg font-semibold text-text-primary">Change Password</h3>
          <p class="text-sm text-text-muted mt-1">Ensure your account is using a secure password</p>
        </div>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          @submit.prevent="handlePasswordChange"
          class="space-y-1"
        >
          <el-form-item label="Current Password" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              placeholder="Enter your current password"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item label="New Password" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              placeholder="Enter new password (min 6 characters)"
              size="large"
              show-password
            />
          </el-form-item>

          <el-form-item label="Confirm New Password" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              placeholder="Confirm your new password"
              size="large"
              show-password
            />
          </el-form-item>

          <div class="pt-2">
            <el-button
              type="primary"
              size="large"
              :loading="passwordLoading"
              @click="handlePasswordChange"
            >
              Update Password
            </el-button>
          </div>
        </el-form>
      </div>
    </template>
  </div>
</template>
import request from './request'

/**
 * Get current user's profile information.
 * @returns {Promise<{id: number, username: string, nickname: string, email: string, avatar: string}>}
 */
export function getProfile() {
  return request.get('/user/profile')
}

/**
 * Update user profile (nickname, email, avatar).
 * @param {Object} data - { nickname?: string, email?: string, avatar?: string }
 * @returns {Promise<{id: number, username: string, nickname: string, email: string, avatar: string}>}
 */
export function updateProfile(data) {
  return request.put('/user/profile', data)
}

/**
 * Upload avatar image file.
 * @param {File} file - The image file to upload
 * @returns {Promise<{avatar: string}>}
 */
export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/**
 * Change user password.
 * @param {Object} data - { oldPassword: string, newPassword: string, confirmPassword: string }
 * @returns {Promise<void>}
 */
export function changePassword(data) {
  return request.put('/user/password', data)
}

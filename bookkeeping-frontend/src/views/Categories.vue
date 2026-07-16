<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { getCategories, createCategory, updateCategory, deleteCategory } from '../api/category'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const filterType = ref(null)

const dialogVisible = ref(false)
const dialogTitle = ref('Add New Category')
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)

const formRef = ref(null)
const form = reactive({ name: '', type: 'INCOME' })

const rules = {
  name: [
    { required: true, message: 'Please enter a category name', trigger: 'blur' },
    { max: 50, message: 'Name is too long (max 50 characters)', trigger: 'blur' },
  ],
  type: [{ required: true, message: 'Please select a type', trigger: 'change' }],
}

function isIncomeType(cat) {
  const t = cat.type
  return t === 'INCOME' || t === 'income' || t === 1 || t === '1'
}

function typeLabel(cat) {
  return isIncomeType(cat) ? 'Income' : 'Expense'
}

const incomeCount = computed(() => tableData.value.filter(isIncomeType).length)
const expenseCount = computed(() => tableData.value.filter((c) => !isIncomeType(c)).length)

const filteredData = computed(() => {
  if (!filterType.value) return tableData.value
  if (filterType.value === 'INCOME') return tableData.value.filter(isIncomeType)
  return tableData.value.filter((c) => !isIncomeType(c))
})

async function loadCategories() {
  loading.value = true
  try {
    tableData.value = await getCategories() || []
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  dialogTitle.value = 'Add New Category'
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.type = 'INCOME'
  dialogVisible.value = true
}

function openEditDialog(row) {
  dialogTitle.value = 'Edit Category'
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.type = isIncomeType(row) ? 'INCOME' : 'EXPENSE'
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }

  submitting.value = true
  try {
    const payload = { name: form.name.trim(), type: form.type === 'INCOME' ? 1 : 2 }
    if (isEdit.value) {
      await updateCategory(editId.value, payload)
      ElMessage.success('Category updated!')
    } else {
      await createCategory(payload)
      ElMessage.success('Category created!')
    }
    dialogVisible.value = false
    loadCategories()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Could not save category. Please try again.')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `Delete "${row.name}"? Transactions in this category will not be deleted, but the category will be removed.`,
      'Delete Category',
      { confirmButtonText: 'Yes, Delete', cancelButtonText: 'Cancel', type: 'warning', confirmButtonClass: 'el-button--danger' }
    )
    await deleteCategory(row.id)
    ElMessage.success(`"${row.name}" has been deleted.`)
    loadCategories()
  } catch { /* cancelled */ }
}

onMounted(() => { loadCategories() })
</script>

<template>
  <div class="space-y-4 animate-fade-in">
    <!-- Header -->
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
      <div class="flex items-center gap-3 flex-wrap">
        <h2 class="text-lg font-extrabold text-text-primary">Categories</h2>
        <div class="flex items-center gap-3 text-sm text-text-muted">
          <span class="inline-flex items-center gap-1">
            <span class="w-2 h-2 rounded-full bg-income"></span>
            {{ incomeCount }} income
          </span>
          <span class="inline-flex items-center gap-1">
            <span class="w-2 h-2 rounded-full bg-expense"></span>
            {{ expenseCount }} expense
          </span>
        </div>
      </div>
      <div class="flex items-center gap-3 flex-wrap">
        <el-select v-model="filterType" placeholder="All Types" clearable size="default" class="w-36">
          <el-option label="All" :value="null" />
          <el-option label="Income" value="INCOME" />
          <el-option label="Expense" value="EXPENSE" />
        </el-select>
        <el-button type="primary" size="default" @click="openAddDialog" class="!rounded-xl font-semibold">+ Add Category</el-button>
      </div>
    </div>

    <!-- Info Banner for new users -->
    <div v-if="tableData.length === 0 && !loading" class="bg-accent-50 border border-accent-200 rounded-2xl p-4 text-sm text-accent-700">
      <p class="font-semibold mb-1">What are categories?</p>
      <p>Categories help you organize your money. For example: <strong>Salary</strong> for income, <strong>Food</strong> or <strong>Rent</strong> for expenses. Create a few to get started!</p>
    </div>

    <!-- Empty State -->
    <div v-if="tableData.length === 0 && !loading" class="flex flex-col items-center justify-center py-16 text-center">
      <svg class="w-16 h-16 text-text-muted mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
      </svg>
      <h3 class="font-bold text-lg text-text-primary mb-2">No categories yet</h3>
      <p class="text-text-secondary text-sm mb-5 max-w-sm">Create categories to organize your income and expenses.</p>
      <el-button type="primary" @click="openAddDialog" class="!rounded-xl">+ Create Your First Category</el-button>
    </div>

    <!-- Category Cards Grid -->
    <div v-else class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="cat in filteredData"
        :key="cat.id"
        class="bg-white rounded-2xl shadow-card border border-border-light p-5 hover:shadow-card-hover transition-all duration-200 group"
      >
        <div class="flex items-start justify-between mb-3">
          <div class="flex items-center gap-3 min-w-0">
            <div
              class="w-10 h-10 rounded-xl flex items-center justify-center shrink-0"
              :class="isIncomeType(cat) ? 'bg-green-100' : 'bg-rose-100'"
            >
              <svg v-if="isIncomeType(cat)" class="w-5 h-5 text-income" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 13l-5 5m0 0l-5-5m5 5V6" />
              </svg>
              <svg v-else class="w-5 h-5 text-expense" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 11l5-5m0 0l5 5m-5-5v12" />
              </svg>
            </div>
            <div class="min-w-0">
              <p class="font-bold text-text-primary text-base truncate">{{ cat.name }}</p>
              <span
                class="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold"
                :class="isIncomeType(cat) ? 'bg-green-50 text-income' : 'bg-rose-50 text-expense'"
              >{{ typeLabel(cat) }}</span>
            </div>
          </div>
        </div>
        <div class="flex items-center gap-2 pt-3 border-t border-border-light opacity-0 group-hover:opacity-100 transition-opacity duration-200">
          <el-button size="small" @click="openEditDialog(cat)" class="!rounded-lg flex-1">Edit</el-button>
          <el-button size="small" type="danger" @click="handleDelete(cat)" class="!rounded-lg flex-1" plain>Delete</el-button>
        </div>
      </div>
    </div>

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="460px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Category Name" prop="name">
          <el-input v-model="form.name" placeholder="e.g. Salary, Food, Rent" size="large" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="Category Type" prop="type">
          <el-radio-group v-model="form.type" class="w-full">
            <el-radio-button value="INCOME" class="flex-1 text-center">Income</el-radio-button>
            <el-radio-button value="EXPENSE" class="flex-1 text-center">Expense</el-radio-button>
          </el-radio-group>
          <p class="text-xs text-text-muted mt-2">
            <template v-if="form.type === 'INCOME'">Income categories are for money you receive (salary, freelance, gifts).</template>
            <template v-else>Expense categories are for money you spend (food, rent, shopping).</template>
          </p>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="dialogVisible = false" size="large" class="!rounded-xl">Cancel</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit" size="large" class="!rounded-xl">
            {{ isEdit ? 'Update' : 'Create' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
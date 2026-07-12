<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCategories, createCategory, updateCategory, deleteCategory } from '../api/category'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])

const dialogVisible = ref(false)
const dialogTitle = ref('Add Category')
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)

const formRef = ref(null)
const form = reactive({
  name: '',
  type: 1,
})

const rules = {
  name: [{ required: true, message: 'Please enter category name', trigger: 'blur' }],
  type: [{ required: true, message: 'Please select type', trigger: 'change' }],
}

async function loadCategories() {
  loading.value = true
  try {
    tableData.value = await getCategories()
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function openAddDialog() {
  dialogTitle.value = 'Add Category'
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.type = 1
  dialogVisible.value = true
}

function openEditDialog(row) {
  dialogTitle.value = 'Edit Category'
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.type = row.type
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCategory(editId.value, { ...form })
      ElMessage.success('Category updated!')
    } else {
      await createCategory({ ...form })
      ElMessage.success('Category created!')
    }
    dialogVisible.value = false
    loadCategories()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `Delete category "${row.name}"?`,
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await deleteCategory(row.id)
    ElMessage.success('Category deleted!')
    loadCategories()
  } catch {
    // cancelled or error
  }
}

onMounted(() => {
  loadCategories()
})
</script>

<template>
  <div class="space-y-4">
    <!-- Toolbar -->
    <div class="flex items-center justify-between">
      <h2 class="text-base font-semibold text-text-primary">Manage Categories</h2>
      <el-button type="primary" @click="openAddDialog">+ Add Category</el-button>
    </div>

    <!-- Table Card -->
    <div class="bg-surface rounded-lg shadow-sm border border-gray-100 overflow-hidden">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        style="width: 100%"
        empty-text="No categories found"
      >
        <el-table-column prop="name" label="Name" min-width="200" />
        <el-table-column label="Type" width="130">
          <template #default="{ row }">
            <el-tag :type="(row.type === 1 || row.type === 'INCOME') ? 'success' : 'danger'" size="small" effect="plain">
              {{ row.type === 1 || row.type === 'INCOME' ? 'Income' : 'Expense' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">Edit</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- Add / Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="450px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Category Name" prop="name">
          <el-input v-model="form.name" placeholder="e.g. Salary, Food, Rent" size="large" />
        </el-form-item>

        <el-form-item label="Type" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">Income</el-radio>
            <el-radio :value="2">Expense</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">Cancel</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">
          {{ isEdit ? 'Update' : 'Add' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getTransactions, createTransaction, updateTransaction, deleteTransaction } from '../api/transaction'
import { getCategories } from '../api/category'
import { ElMessage, ElMessageBox } from 'element-plus'

// ---------- State ----------
const loading = ref(false)
const tableData = ref([])
const categories = ref([])
const total = ref(0)
const pagination = reactive({ page: 1, pageSize: 10 })
const filterType = ref(null)

// Dialog state
const dialogVisible = ref(false)
const dialogTitle = ref('Add Transaction')
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)

const formRef = ref(null)
const form = reactive({
  type: 1,
  amount: null,
  categoryId: null,
  note: '',
  transactionDate: new Date().toISOString().split('T')[0],
})

const rules = {
  type: [{ required: true, message: 'Please select type', trigger: 'change' }],
  amount: [{ required: true, message: 'Please enter amount', trigger: 'blur' }],
  categoryId: [{ required: true, message: 'Please select category', trigger: 'change' }],
  transactionDate: [{ required: true, message: 'Please select date', trigger: 'change' }],
}

// ---------- Methods ----------
async function loadTransactions() {
  loading.value = true
  try {
    const params = { page: pagination.page, pageSize: pagination.pageSize }
    if (filterType.value) params.type = filterType.value
    const data = await getTransactions(params)
    tableData.value = data?.records || data || []
    total.value = data?.total || tableData.value.length
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    categories.value = await getCategories()
  } catch {
    categories.value = []
  }
}

function handlePageChange(page) {
  pagination.page = page
  loadTransactions()
}

function handleSizeChange(size) {
  pagination.pageSize = size
  pagination.page = 1
  loadTransactions()
}

function openAddDialog() {
  dialogTitle.value = 'Add Transaction'
  isEdit.value = false
  editId.value = null
  form.type = 1
  form.amount = null
  form.categoryId = null
  form.note = ''
  form.transactionDate = new Date().toISOString().split('T')[0]
  dialogVisible.value = true
}

function openEditDialog(row) {
  dialogTitle.value = 'Edit Transaction'
  isEdit.value = true
  editId.value = row.id
  form.type = row.type
  form.amount = row.amount
  form.categoryId = row.categoryId
  form.note = row.note || row.description || ''
  form.transactionDate = row.transactionDate
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
    const payload = {
      type: form.type,
      amount: form.amount,
      categoryId: form.categoryId,
      transactionDate: form.transactionDate,
      note: form.note || null,
    }
    if (isEdit.value) {
      await updateTransaction(editId.value, payload)
      ElMessage.success('Transaction updated!')
    } else {
      await createTransaction(payload)
      ElMessage.success('Transaction added!')
    }
    dialogVisible.value = false
    loadTransactions()
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `Delete transaction "${row.note || row.description || 'this transaction'}"?`,
      'Confirm Delete',
      { confirmButtonText: 'Delete', cancelButtonText: 'Cancel', type: 'warning' }
    )
    await deleteTransaction(row.id)
    ElMessage.success('Transaction deleted!')
    loadTransactions()
  } catch {
    // cancelled or error
  }
}

function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}

function getCategoryName(categoryId) {
  const cat = categories.value.find((c) => c.id === categoryId)
  return cat ? cat.name : '-'
}

function isIncome(type) {
  return type === 1 || type === 'INCOME' || type === 'income'
}

function typeLabel(type) {
  return isIncome(type) ? 'Income' : 'Expense'
}

onMounted(() => {
  loadCategories()
  loadTransactions()
})
</script>

<template>
  <div class="space-y-4">
    <!-- Toolbar -->
    <div class="flex flex-wrap items-center justify-between gap-3">
      <div class="flex items-center gap-3">
        <el-select v-model="filterType" placeholder="All Types" clearable size="default" class="w-36" @change="loadTransactions">
          <el-option label="All" :value="null" />
          <el-option label="Income" :value="1" />
          <el-option label="Expense" :value="2" />
        </el-select>
      </div>
      <el-button type="primary" @click="openAddDialog">
        + Add Transaction
      </el-button>
    </div>

    <!-- Table Card -->
    <div class="bg-surface rounded-lg shadow-sm border border-gray-100 overflow-hidden">
      <el-table
        v-loading="loading"
        :data="tableData"
        stripe
        style="width: 100%"
        empty-text="No transactions found"
      >
        <el-table-column prop="transactionDate" label="Date" width="130" />
        <el-table-column label="Type" width="100">
          <template #default="{ row }">
            <el-tag :type="isIncome(row.type) ? 'success' : 'danger'" size="small" effect="plain">
              {{ typeLabel(row.type) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Category" min-width="130">
          <template #default="{ row }">
            {{ getCategoryName(row.categoryId) }}
          </template>
        </el-table-column>
        <el-table-column label="Note" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.note || row.description || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="Amount" width="150" align="right">
          <template #default="{ row }">
            <span
              class="font-semibold"
              :class="isIncome(row.type) ? 'text-primary' : 'text-danger'"
            >
              {{ isIncome(row.type) ? '+' : '-' }}{{ formatCurrency(row.amount) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="140" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">Edit</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">Delete</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- Pagination -->
      <div class="flex justify-end p-4 border-t border-gray-100">
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- Add / Edit Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Type" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio :value="1">Income</el-radio>
            <el-radio :value="2">Expense</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Amount" prop="amount">
          <el-input-number
            v-model="form.amount"
            :min="0"
            :precision="2"
            class="w-full"
            placeholder="0.00"
          />
        </el-form-item>

        <el-form-item label="Category" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="Select category" class="w-full">
            <el-option
              v-for="cat in categories"
              :key="cat.id"
              :label="cat.name"
              :value="cat.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="Date" prop="transactionDate">
          <el-date-picker
            v-model="form.transactionDate"
            type="date"
            placeholder="Select date"
            class="w-full"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>

        <el-form-item label="Note">
          <el-input
            v-model="form.note"
            type="textarea"
            :rows="2"
            placeholder="Optional note..."
          />
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
<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getTransactions, createTransaction, updateTransaction, deleteTransaction } from '../api/transaction'
import { getCategories } from '../api/category'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const loading = ref(false)
const tableData = ref([])
const categories = ref([])
const total = ref(0)
const pagination = reactive({ page: 1, pageSize: 10 })
const filterType = ref(null)
const filterCategoryId = ref(null)
const filterCategoryName = ref('')
const filterStartDate = ref('')
const filterEndDate = ref('')
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('Add New Transaction')
const isEdit = ref(false)
const editId = ref(null)
const submitting = ref(false)

const formRef = ref(null)
const form = reactive({
  type: 'INCOME',
  amount: null,
  categoryId: null,
  note: '',
  transactionDate: new Date().toISOString().split('T')[0],
})

const rules = {
  type: [{ required: true, message: 'Please select a type', trigger: 'change' }],
  amount: [
    { required: true, message: 'Please enter an amount', trigger: 'blur' },
    { type: 'number', min: 0.01, message: 'Amount must be greater than 0', trigger: 'blur' },
  ],
  categoryId: [{ required: true, message: 'Please pick a category', trigger: 'change' }],
  transactionDate: [{ required: true, message: 'Please select a date', trigger: 'change' }],
}

const currencySymbols = { CNY: '¥', USD: '$', IDR: 'Rp', SGD: 'S$', AUD: 'A$', EUR: '€', GBP: '£', JPY: '¥', MAD: 'MAD', RUB: '₽' }

function formatAmount(amount, currency = 'CNY') {
  const sym = currencySymbols[currency] || ''
  return `${sym} ${Number(amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

function getCategoryName(categoryId) {
  const cat = categories.value.find((c) => c.id === categoryId)
  return cat ? cat.name : '—'
}

const dialogFilteredCategories = computed(() => {
  const t = form.type
  if (!t) return categories.value
  return categories.value.filter((c) => (c.type === 1 || c.type === 'INCOME') ? t === 'INCOME' : t === 'EXPENSE')
})

function isIncome(tx) {
  const t = tx.type
  return t === 'INCOME' || t === 'income' || t === 1 || t === '1'
}

function typeLabel(tx) {
  return isIncome(tx) ? 'Income' : 'Expense'
}

function displayLabel(tx) {
  return tx.note || tx.description || getCategoryName(tx.categoryId) || 'Transaction'
}

const filteredData = computed(() => {
  if (!searchKeyword.value) return tableData.value
  const kw = searchKeyword.value.toLowerCase()
  return tableData.value.filter((tx) => {
    return (
      (tx.note || '').toLowerCase().includes(kw) ||
      (tx.description || '').toLowerCase().includes(kw) ||
      getCategoryName(tx.categoryId).toLowerCase().includes(kw) ||
      (tx.transactionDate || '').includes(kw)
    )
  })
})

async function loadTransactions() {
  loading.value = true
  try {
    const params = { page: pagination.page, pageSize: pagination.pageSize }
    if (filterType.value) params.type = filterType.value === 'INCOME' ? 1 : 2
    if (filterCategoryId.value) params.categoryId = filterCategoryId.value
    if (filterStartDate.value) params.startDate = filterStartDate.value
    if (filterEndDate.value) params.endDate = filterEndDate.value
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

function handlePageChange(page) { pagination.page = page; loadTransactions() }
function handleSizeChange(size) { pagination.pageSize = size; pagination.page = 1; loadTransactions() }

function openAddDialog() {
  dialogTitle.value = 'Add New Transaction'
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEditDialog(row) {
  dialogTitle.value = 'Edit Transaction'
  isEdit.value = true
  editId.value = row.id
  form.type = isIncome(row) ? 'INCOME' : 'EXPENSE'
  form.amount = row.amount
  form.categoryId = row.categoryId
  form.note = row.note || row.description || ''
  form.transactionDate = row.transactionDate
  dialogVisible.value = true
}

function resetForm() {
  form.type = 'INCOME'
  form.amount = null
  form.categoryId = null
  form.note = ''
  form.transactionDate = new Date().toISOString().split('T')[0]
  formRef.value?.resetFields()
}

async function handleSubmit() {
  if (!formRef.value) return
  try { await formRef.value.validate() } catch { return }

  submitting.value = true
  try {
    const payload = {
      type: form.type === 'INCOME' ? 1 : 2,
      amount: Number(form.amount),
      categoryId: form.categoryId,
      transactionDate: form.transactionDate,
      note: form.note || null,
    }
    if (isEdit.value) {
      await updateTransaction(editId.value, payload)
      ElMessage.success('Transaction updated successfully!')
    } else {
      await createTransaction(payload)
      ElMessage.success('Transaction added!')
    }
    dialogVisible.value = false
    loadTransactions()
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Something went wrong. Please try again.')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `Delete "${displayLabel(row)}"? This action cannot be undone.`,
      'Delete Transaction',
      { confirmButtonText: 'Yes, Delete', cancelButtonText: 'Cancel', type: 'warning', confirmButtonClass: 'el-button--danger' }
    )
    await deleteTransaction(row.id)
    ElMessage.success('Transaction deleted.')
    loadTransactions()
  } catch { /* cancelled */ }
}

function applyCategoryFromQuery() {
  const qCatId = route.query.categoryId
  const qCatName = route.query.categoryName
  if (qCatId) {
    filterCategoryId.value = Number(qCatId)
    filterCategoryName.value = qCatName || ''
  }
}

watch(() => route.query.categoryId, (newVal) => {
  if (newVal) {
    filterCategoryId.value = Number(newVal)
    filterCategoryName.value = route.query.categoryName || ''
    pagination.page = 1
    loadTransactions()
  } else {
    clearCategoryFilter()
  }
})

function clearCategoryFilter() {
  filterCategoryId.value = null
  filterCategoryName.value = ''
  pagination.page = 1
  loadTransactions()
}

onMounted(() => { loadCategories(); applyCategoryFromQuery(); loadTransactions() })
</script>

<template>
  <div class="space-y-4 animate-fade-in">
    <!-- Category filter banner from drill-down -->
    <div v-if="filterCategoryId && filterCategoryName" class="flex items-center gap-2 bg-primary-50 border border-primary-200 rounded-xl px-4 py-2.5 text-sm">
      <svg class="w-4 h-4 text-primary-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z" />
      </svg>
      <span class="text-primary-700 font-medium">Category:</span>
      <el-tag type="primary" size="small" effect="plain">{{ filterCategoryName }}</el-tag>
      <el-button size="small" @click="clearCategoryFilter" class="!rounded-lg" plain>Clear</el-button>
    </div>

    <!-- Toolbar -->
    <div class="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3">
      <div class="flex items-center gap-3 flex-wrap">
        <el-select v-model="filterType" placeholder="All Types" clearable size="default" class="w-36" @change="loadTransactions">
          <el-option label="All" :value="null" />
          <el-option label="Income" value="INCOME" />
          <el-option label="Expense" value="EXPENSE" />
        </el-select>
        <el-date-picker v-model="filterStartDate" type="date" placeholder="Start date" size="default" class="w-36" value-format="YYYY-MM-DD" @change="loadTransactions" />
        <el-date-picker v-model="filterEndDate" type="date" placeholder="End date" size="default" class="w-36" value-format="YYYY-MM-DD" @change="loadTransactions" />
        <el-input v-model="searchKeyword" placeholder="Search transactions..." clearable size="default" class="w-52">
          <template #prefix>
            <svg class="w-4 h-4 text-text-muted" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </template>
        </el-input>
      </div>
      <el-button type="primary" size="default" @click="openAddDialog" class="!rounded-xl font-semibold">+ Add Transaction</el-button>
    </div>

    <!-- Table Card -->
    <div class="bg-white rounded-2xl shadow-card border border-border-light overflow-hidden">
      <div v-if="!loading && tableData.length === 0" class="flex flex-col items-center justify-center py-16 px-5 text-center">
        <svg class="w-16 h-16 text-text-muted mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" />
        </svg>
        <h3 class="font-bold text-lg text-text-primary mb-2">No transactions found</h3>
        <p class="text-text-secondary text-sm mb-5 max-w-sm">
          {{ filterType || searchKeyword ? 'Try adjusting your filters or search terms.' : 'Start tracking your money by adding your first transaction.' }}
        </p>
        <el-button v-if="!filterType && !searchKeyword" type="primary" @click="openAddDialog" class="!rounded-xl">+ Record Your First Transaction</el-button>
      </div>

      <el-table
        v-else
        v-loading="loading"
        :data="filteredData"
        stripe
        style="width: 100%"
        empty-text="No matching transactions"
      >
        <el-table-column prop="transactionDate" label="Date" width="120" sortable />
        <el-table-column label="Description" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <svg v-if="isIncome(row)" class="w-4 h-4 text-income shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 13l-5 5m0 0l-5-5m5 5V6" />
              </svg>
              <svg v-else class="w-4 h-4 text-expense shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 11l5-5m0 0l5 5m-5-5v12" />
              </svg>
              <span class="text-sm font-medium text-text-primary">{{ displayLabel(row) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="Category" width="140">
          <template #default="{ row }">
            <el-tag :type="isIncome(row) ? 'success' : 'danger'" size="small" effect="plain" class="font-semibold">
              {{ getCategoryName(row.categoryId) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="Type" width="100">
          <template #default="{ row }">
            <span class="inline-flex items-center gap-1 px-2.5 py-1 rounded-full text-xs font-bold"
              :class="isIncome(row) ? 'bg-green-50 text-income' : 'bg-rose-50 text-expense'"
            >{{ typeLabel(row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="Amount" width="170" align="right">
          <template #default="{ row }">
            <span class="text-sm font-bold tabular-nums" :class="isIncome(row) ? 'text-income' : 'text-expense'">
              {{ isIncome(row) ? '+' : '−' }}{{ formatAmount(row.amount, row.currency || 'CNY') }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="Actions" width="140" fixed="right" align="center">
          <template #default="{ row }">
            <div class="flex items-center justify-center gap-1">
              <el-button type="primary" link size="small" @click="openEditDialog(row)">Edit</el-button>
              <el-button type="danger" link size="small" @click="handleDelete(row)">Delete</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="tableData.length > 0" class="flex justify-end px-5 py-4 border-t border-border-light">
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

    <!-- Dialog -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="Type" prop="type">
          <el-radio-group v-model="form.type" class="w-full">
            <el-radio-button value="INCOME" class="flex-1 text-center">Income</el-radio-button>
            <el-radio-button value="EXPENSE" class="flex-1 text-center">Expense</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Amount" prop="amount">
          <el-input v-model.number="form.amount" type="number" :min="0" :precision="2" placeholder="Enter amount" size="large" />
        </el-form-item>
        <el-form-item label="Category" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="Choose a category" class="w-full" size="large">
            <el-option v-for="cat in dialogFilteredCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Date" prop="transactionDate">
          <el-date-picker v-model="form.transactionDate" type="date" placeholder="Pick a date" class="w-full" value-format="YYYY-MM-DD" size="large" />
        </el-form-item>
        <el-form-item label="Note (Optional)">
          <el-input v-model="form.note" type="textarea" :rows="2" placeholder="Add a short note about this transaction..." maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="dialogVisible = false" size="large" class="!rounded-xl">Cancel</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit" size="large" class="!rounded-xl">
            {{ isEdit ? 'Update' : 'Save' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
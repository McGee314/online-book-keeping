<script setup>
import { ref, onMounted, inject } from 'vue'
import { ElMessage } from 'element-plus'
import { getTransactions, createTransaction } from '../api/transaction'
import { getCategories } from '../api/category'

/* ───────── provide / inject for header "Add Transaction" ───────── */
const openAddTransaction = inject('onAddTransaction', () => {})

/* ───────── reactive state ───────── */
const stats = ref({
  income: 0,
  expense: 0,
  balance: 0,
  transactionCount: 0,
  categoryCount: 0,
})

const recentTransactions = ref([])
const categories = ref([])
const loading = ref(true)

/* ───────── dialog / form state ───────── */
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = ref({
  amount: null,
  type: 1,       // 1 = INCOME, 2 = EXPENSE
  categoryId: null,
  transactionDate: new Date().toISOString().slice(0, 10),
})

const formRules = {
  amount: [
    { required: true, message: 'Amount is required', trigger: 'blur' },
    { type: 'number', min: 0.01, message: 'Must be greater than 0', trigger: 'blur' },
  ],
  type: [
    { required: true, message: 'Type is required', trigger: 'change' },
  ],
  categoryId: [
    { required: true, message: 'Category is required', trigger: 'change' },
  ],
  transactionDate: [
    { required: true, message: 'Date is required', trigger: 'blur' },
  ],
}

/* ───────── fetch data ───────── */
async function fetchData() {
  loading.value = true
  try {
    const [txData, catData] = await Promise.all([
      getTransactions({ page: 1, pageSize: 5 }),
      getCategories(),
    ])

    const transactions = txData?.records || txData || []
    categories.value = catData || []

    let income = 0
    let expense = 0

    transactions.forEach((t) => {
      if (t.type === 'INCOME' || t.type === 'income' || t.type === 1 || t.type === '1') {
        income += Number(t.amount || 0)
      } else {
        expense += Number(t.amount || 0)
      }
    })

    stats.value = {
      income,
      expense,
      balance: income - expense,
      transactionCount: txData?.total || transactions.length,
      categoryCount: (catData || []).length,
    }

    recentTransactions.value = (txData?.records || transactions).slice(0, 5)
  } catch {
    // use empty/default values
  } finally {
    loading.value = false
  }
}

/* ───────── dialog helpers ───────── */
function openDialog() {
  resetForm()
  dialogVisible.value = true
}

function resetForm() {
  form.value = {
    amount: null,
    type: 1,
    categoryId: null,
    transactionDate: new Date().toISOString().slice(0, 10),
  }
  formRef.value?.resetFields()
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await createTransaction({
      amount: Number(form.value.amount),
      type: form.value.type,
      categoryId: form.value.categoryId,
      transactionDate: form.value.transactionDate,
    })
    ElMessage.success('Transaction added successfully!')
    dialogVisible.value = false
    await fetchData() // refresh dashboard
  } catch (err) {
    const msg = err?.response?.data?.message || err?.message || 'Failed to add transaction'
    ElMessage.error(msg)
  } finally {
    submitting.value = false
  }
}

/* ───────── format ───────── */
function formatCurrency(value) {
  return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(value)
}

function typeLabel(type) {
  // Handles string enum values from backend
  if (type === 'INCOME' || type === 'income' || type === 1 || type === '1') return 'Income'
  return 'Expense'
}

/* ───────── lifecycle ───────── */
onMounted(() => {
  openAddTransaction(openDialog) // register handler so header button works
  fetchData()
})
</script>

<template>
  <div v-loading="loading">
    <!-- ═══════════ Summary Cards ═══════════ -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <!-- Income card — very light green background -->
      <div class="rounded-lg shadow-sm p-5 border border-green-200 bg-green-50">
        <div class="text-text-secondary text-sm mb-1">Income</div>
        <div class="text-2xl font-bold text-primary">{{ formatCurrency(stats.income) }}</div>
      </div>
      <!-- Expense card — very light pink/red background -->
      <div class="rounded-lg shadow-sm p-5 border border-red-200 bg-red-50">
        <div class="text-text-secondary text-sm mb-1">Expense</div>
        <div class="text-2xl font-bold text-danger">{{ formatCurrency(stats.expense) }}</div>
      </div>
      <!-- Balance card — neutral background -->
      <div class="bg-surface rounded-lg shadow-sm p-5 border border-gray-100">
        <div class="text-text-secondary text-sm mb-1">Balance</div>
        <div class="text-2xl font-bold" :class="stats.balance >= 0 ? 'text-primary' : 'text-danger'">
          {{ formatCurrency(stats.balance) }}
        </div>
      </div>
      <!-- Transactions count card — neutral background -->
      <div class="bg-surface rounded-lg shadow-sm p-5 border border-gray-100">
        <div class="text-text-secondary text-sm mb-1">Transactions</div>
        <div class="text-2xl font-bold text-text-primary">{{ stats.transactionCount }}</div>
        <div class="text-text-secondary text-xs mt-1">{{ stats.categoryCount }} categories</div>
      </div>
    </div>

    <!-- ═══════════ Recent Transactions (two columns) ═══════════ -->
    <div class="flex flex-col lg:flex-row gap-6">
      <!-- Left column — el-table (60%) -->
      <div class="flex-[3] bg-surface rounded-lg shadow-sm border border-gray-100">
        <div class="px-5 py-4 border-b border-gray-100">
          <h2 class="text-base font-semibold text-text-primary">Recent Transactions</h2>
        </div>
        <el-table
          v-if="recentTransactions.length > 0"
          :data="recentTransactions"
          stripe
          size="small"
          style="width: 100%"
        >
          <el-table-column prop="description" label="Description" min-width="140">
            <template #default="{ row }">
              <span class="text-sm text-text-primary">
                {{ row.description || row.categoryName || 'Transaction' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="type" label="Type" width="100" align="center">
            <template #default="{ row }">
              <el-tag
                :type="(row.type === 'INCOME' || row.type === 'income' || row.type === 1) ? 'success' : 'danger'"
                size="small"
                effect="plain"
              >
                {{ typeLabel(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="amount" label="Amount" width="140" align="right">
            <template #default="{ row }">
              <span
                class="text-sm font-semibold"
                :class="(row.type === 'INCOME' || row.type === 'income' || row.type === 1) ? 'text-primary' : 'text-danger'"
              >
                {{ (row.type === 'INCOME' || row.type === 'income' || row.type === 1) ? '+' : '-' }}{{ formatCurrency(row.amount) }}
              </span>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="px-5 py-8 text-center text-text-secondary text-sm">
          No transactions yet. Add your first transaction!
        </div>
      </div>

      <!-- Right column — Pie Chart placeholder (40%) -->
      <div class="flex-[2] bg-surface rounded-lg shadow-sm border border-gray-100 p-5 flex flex-col items-center justify-center min-h-[260px]">
        <div class="text-center text-text-secondary">
          <svg class="mx-auto mb-3" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#c0c4cc" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21.21 15.89A10 10 0 1 1 8 2.83" />
            <path d="M22 12A10 10 0 0 0 12 2v10z" />
          </svg>
          <p class="text-sm font-medium text-text-regular">Pie Chart Area</p>
          <p class="text-xs text-text-secondary mt-1">ECharts visualisation coming soon</p>
        </div>
      </div>
    </div>

    <!-- ═══════════ Add Transaction Dialog ═══════════ -->
    <el-dialog
      v-model="dialogVisible"
      title="Add Transaction"
      width="520px"
      :close-on-click-modal="false"
      @closed="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-position="top"
        label-width="auto"
        @submit.prevent="handleSubmit"
      >
        <el-form-item label="Amount" prop="amount">
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :precision="2"
            :controls="true"
            style="width: 100%"
            placeholder="0.00"
          />
        </el-form-item>

        <el-form-item label="Type" prop="type">
          <el-radio-group v-model="form.type">
            <el-radio-button :value="1">Income</el-radio-button>
            <el-radio-button :value="2">Expense</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="Category" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="Select a category" style="width: 100%">
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
            placeholder="Pick a date"
            style="width: 100%"
            value-format="YYYY-MM-DD"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="dialogVisible = false">Cancel</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            Save Transaction
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
<script setup>
import { ref, computed, onMounted, inject, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getTransactions, createTransaction } from '../api/transaction'
import { getCategories } from '../api/category'
import { getRates } from '../api/currency'
import { getByCategory } from '../api/report'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, GraphicComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([PieChart, TitleComponent, TooltipComponent, GraphicComponent, CanvasRenderer])

/* ───────── provide / inject ───────── */
const openAddTransaction = inject('onAddTransaction', () => {})
const displayCurrency = inject('displayCurrency', ref('IDR'))

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
  type: 1,
  categoryId: null,
  currency: 'CNY',
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
  currency: [
    { required: true, message: 'Currency is required', trigger: 'change' },
  ],
  transactionDate: [
    { required: true, message: 'Date is required', trigger: 'blur' },
  ],
}

/* ───────── live exchange rates from backend ───────── */
// rawRates: { CNY: idrPerCny, USD: idrPerUsd }  e.g. { CNY: 0.000375, USD: 0.000062 }
// null means rates haven't loaded; set to the object once API responds successfully
const rawRates = ref(null)
const ratesError = ref('')

// Full cross-rate table computed from rawRates (null if rates unavailable)
const conversionRates = computed(() => {
  if (!rawRates.value) return null
  const idrToCny = Number(rawRates.value.CNY)
  const idrToUsd = Number(rawRates.value.USD)
  const cnyToIdr = idrToCny > 0 ? 1 / idrToCny : 0
  const usdToIdr = idrToUsd > 0 ? 1 / idrToUsd : 0
  if (cnyToIdr === 0 || usdToIdr === 0) return null
  return {
    IDR: {
      CNY: idrToCny,
      USD: idrToUsd,
    },
    CNY: {
      IDR: cnyToIdr,
      USD: idrToCny > 0 ? idrToUsd / idrToCny : 0,
    },
    USD: {
      IDR: usdToIdr,
      CNY: idrToUsd > 0 ? idrToCny / idrToUsd : 0,
    },
  }
})

async function fetchRates() {
  ratesError.value = ''
  try {
    const data = await getRates('IDR')
    if (data) {
      rawRates.value = {
        CNY: Number(data.CNY),
        USD: Number(data.USD),
      }
    }
  } catch (err) {
    ratesError.value = err?.response?.data?.message || 'Live exchange rates unavailable'
    rawRates.value = null
  }
}

function convertAmount(amount, fromCurrency, toCurrency) {
  if (!conversionRates.value) return null
  if (fromCurrency === toCurrency) return null
  const rate = conversionRates.value[fromCurrency]?.[toCurrency]
  if (!rate) return null
  return (Number(amount || 0) * rate).toFixed(2)
}

function formatConverted(amount, fromCurrency, toCurrency) {
  if (!conversionRates.value) return ''
  const converted = convertAmount(amount, fromCurrency, toCurrency)
  if (converted === null) return ''
  const symbol = toCurrency === 'IDR' ? 'Rp' : toCurrency === 'CNY' ? '¥' : '$'
  return `~ ${symbol} ${Number(converted).toLocaleString('en-US')}`
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
    currency: 'CNY',
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
      currencyCode: form.value.currency,
      transactionDate: form.value.transactionDate,
    })
    ElMessage.success('Transaction added successfully!')
    dialogVisible.value = false
    await Promise.all([fetchData(), fetchChartData()])
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

function formatAmount(value) {
  return new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value)
}

function currencySymbol(code) {
  if (code === 'IDR') return 'Rp'
  if (code === 'CNY') return '¥'
  return '$'
}

function typeLabel(type) {
  if (type === 'INCOME' || type === 'income' || type === 1 || type === '1') return 'Income'
  return 'Expense'
}

function isIncomeType(type) {
  return type === 'INCOME' || type === 'income' || type === 1 || type === '1'
}

/* ───────── donut chart state ───────── */
const chartType = ref(2) // 2=Expense, 1=Income
const chartData = ref([])
const chartLoading = ref(false)

const expenseColors = ['#F56C6C', '#E6A23C', '#F8981D', '#FF6B6B', '#EE5A24', '#EA2027', '#C44569', '#B33771', '#9B59B6', '#8E44AD']
const incomeColors = ['#67C23A', '#529B2E', '#4CAF50', '#26A69A', '#00BCD4', '#0097A6', '#1ABC9C', '#2ECC71', '#27AE60', '#16A085']

const chartColors = computed(() => chartType.value === 2 ? expenseColors : incomeColors)

const chartOption = computed(() => {
  const data = chartData.value
  if (!data || data.length === 0) return {}

  const total = data.reduce((s, d) => s + Number(d.totalAmount || 0), 0)
  const formattedTotal = new Intl.NumberFormat('en-US', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(total)

  return {
    tooltip: {
      trigger: 'item',
      formatter: (p) => {
        return `${p.name}<br/><strong>Rp ${p.value.toLocaleString('en-US')}</strong> (${p.percent}%)`
      },
    },
    graphic: [
      {
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text: `Total\nRp ${formattedTotal}`,
          textAlign: 'center',
          fill: '#333',
          fontSize: 13,
          lineHeight: 20,
          fontWeight: 'bold',
        },
      },
    ],
    series: [
      {
        type: 'pie',
        radius: ['50%', '70%'],
        center: ['50%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: {
          show: false,
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 14,
            fontWeight: 'bold',
          },
          scaleSize: 8,
        },
        data: data.map((d, i) => ({
          name: d.categoryName,
          value: Number(d.totalAmount),
          itemStyle: { color: chartColors.value[i % chartColors.value.length] },
        })),
        animationType: 'scale',
        animationEasing: 'elasticOut',
        animationDuration: 800,
      },
    ],
  }
})

async function fetchChartData() {
  chartLoading.value = true
  try {
    const data = await getByCategory(chartType.value)
    chartData.value = data || []
  } catch {
    chartData.value = []
  } finally {
    chartLoading.value = false
  }
}

watch(chartType, () => {
  fetchChartData()
})

/* ───────── lifecycle ───────── */
onMounted(() => {
  openAddTransaction(openDialog)
  fetchRates()
  fetchData()
  fetchChartData()
})
</script>

<template>
  <div v-loading="loading">
    <!-- ═══════════ Summary Cards ═══════════ -->
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
      <div class="rounded-lg shadow-sm p-5 border border-green-200 bg-green-50">
        <div class="text-text-secondary text-sm mb-1">Income</div>
        <div class="text-2xl font-bold text-primary">{{ formatCurrency(stats.income) }}</div>
      </div>
      <div class="rounded-lg shadow-sm p-5 border border-red-200 bg-red-50">
        <div class="text-text-secondary text-sm mb-1">Expense</div>
        <div class="text-2xl font-bold text-danger">{{ formatCurrency(stats.expense) }}</div>
      </div>
      <div class="bg-surface rounded-lg shadow-sm p-5 border border-gray-100">
        <div class="text-text-secondary text-sm mb-1">Balance</div>
        <div class="text-2xl font-bold" :class="stats.balance >= 0 ? 'text-primary' : 'text-danger'">
          {{ formatCurrency(stats.balance) }}
        </div>
      </div>
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
          <el-table-column label="Description" min-width="140">
            <template #default="{ row }">
              <span class="text-sm text-text-primary">
                {{ row.note || row.description || row.categoryName || 'Transaction' }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="Type" width="90" align="center">
            <template #default="{ row }">
              <el-tag
                :type="isIncomeType(row.type) ? 'success' : 'danger'"
                size="small"
                effect="plain"
              >
                {{ typeLabel(row.type) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="Amount" width="170" align="right">
            <template #default="{ row }">
              <div class="flex flex-col items-end leading-tight">
                <span
                  class="text-sm font-semibold"
                  :class="isIncomeType(row.type) ? 'text-primary' : 'text-danger'"
                >
                  {{ isIncomeType(row.type) ? '+' : '-' }}{{ currencySymbol(row.currency || 'CNY') }} {{ formatAmount(row.amount) }}
                </span>
                <template v-if="(row.currency || 'CNY') !== displayCurrency">
                  <span
                    v-if="ratesError"
                    class="text-xs text-orange-400 mt-0.5"
                  >
                    {{ ratesError }}
                  </span>
                  <span
                    v-else-if="conversionRates"
                    class="text-xs text-gray-400 mt-0.5"
                  >
                    ({{ formatConverted(row.amount, row.currency || 'CNY', displayCurrency) }})
                  </span>
                </template>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="px-5 py-8 text-center text-text-secondary text-sm">
          No transactions yet. Add your first transaction!
        </div>
      </div>

      <!-- Right column — Donut Chart (40%) -->
      <div class="flex-[2] bg-surface rounded-lg shadow-sm border border-gray-100 p-5 flex flex-col min-h-[260px]">
        <!-- Toggle -->
        <div class="flex items-center justify-center mb-3">
          <el-radio-group v-model="chartType" size="small">
            <el-radio-button :value="2">Expense</el-radio-button>
            <el-radio-button :value="1">Income</el-radio-button>
          </el-radio-group>
        </div>
        <!-- Chart -->
        <div v-loading="chartLoading" class="flex-1 flex items-center justify-center">
          <v-chart
            v-if="chartData.length > 0"
            :option="chartOption"
            style="width: 100%; height: 240px;"
            autoresize
          />
          <div v-else class="text-center text-text-secondary">
            <p class="text-sm">{{ chartType === 2 ? 'No expense data yet' : 'No income data yet' }}</p>
          </div>
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
          <el-input
            v-model="form.amount"
            placeholder="0.00"
            type="number"
            min="0.01"
            step="0.01"
          >
            <template #prepend>
              <el-select
                v-model="form.currency"
                class="currency-input-select"
                style="width: 90px"
              >
                <el-option label="IDR" value="IDR" />
                <el-option label="CNY" value="CNY" />
                <el-option label="USD" value="USD" />
              </el-select>
            </template>
          </el-input>
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
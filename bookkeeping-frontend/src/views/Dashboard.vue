<script setup>
import { ref, computed, onMounted, inject, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTransactions, createTransaction } from '../api/transaction'
import { getCategories } from '../api/category'
import { getRates } from '../api/currency'
import { getByCategory, getDailyTrend, getStats } from '../api/report'
import { getCurrentBudget, setBudget } from '../api/budget'

import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart, LineChart } from 'echarts/charts'
import { TitleComponent, TooltipComponent, LegendComponent, GraphicComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([PieChart, LineChart, TitleComponent, TooltipComponent, LegendComponent, GraphicComponent, GridComponent, CanvasRenderer])

/* ───────── provide / inject ───────── */
const router = useRouter()
const openAddTransaction = inject('onAddTransaction', () => {})
const displayCurrency = inject('displayCurrency', ref('CNY'))

/* ───────── state ───────── */
const loading = ref(true)
const stats = ref({ income: 0, expense: 0, balance: 0, transactionCount: 0, categoryCount: 0, budget: null, budgetPercent: null })
const recentTransactions = ref([])
const categories = ref([])
const rawRates = ref(null)
const ratesError = ref('')

/* ───────── dialog ───────── */
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref(null)
const form = ref({
  amount: null,
  type: 'INCOME',
  categoryId: null,
  note: '',
  transactionDate: new Date().toISOString().slice(0, 10),
})

const formRules = {
  amount: [
    { required: true, message: 'Please enter an amount', trigger: 'blur' },
    { type: 'number', min: 0.01, message: 'Amount must be greater than 0', trigger: 'blur' },
  ],
  type: [{ required: true, message: 'Please select a type', trigger: 'change' }],
  categoryId: [{ required: true, message: 'Please select a category', trigger: 'change' }],
  transactionDate: [{ required: true, message: 'Please pick a date', trigger: 'blur' }],
}

/* ───────── chart ───────── */
const chartType = ref('EXPENSE')
const chartData = ref([])
const chartLoading = ref(false)

const chartColors = [
  '#16a34a', '#2563eb', '#f59e0b', '#8b5cf6', '#ec4899',
  '#14b8a6', '#f97316', '#6366f1', '#84cc16', '#06b6d4',
]

const chartOption = computed(() => {
  const data = chartData.value
  if (!data || data.length === 0) return {}
  const total = data.reduce((s, d) => s + Number(d.totalAmount || 0), 0)
  return {
    tooltip: {
      trigger: 'item',
      formatter: (p) => `<strong>${p.name}</strong><br/>¥ ${p.value.toLocaleString('en-US', { minimumFractionDigits: 2 })}<br/><span style="color:#94a3b8">${p.percent}%</span>`,
    },
    legend: { orient: 'vertical', right: 0, top: 'middle', itemWidth: 8, itemHeight: 8, itemGap: 10, textStyle: { fontSize: 11, color: '#64748b' } },
    series: [{
      type: 'pie', radius: ['55%', '80%'], center: ['42%', '50%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 4, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' }, scaleSize: 6 },
      data: data.map((d, i) => ({
        name: d.categoryName, value: Number(d.totalAmount),
        itemStyle: { color: chartColors[i % chartColors.length] },
      })),
    }],
    graphic: [{
      type: 'text', left: '42%', top: 'middle',
      style: {
          text: `¥ ${Number(total).toLocaleString('en-US', { maximumFractionDigits: 0 })}`,
        textAlign: 'center', fill: '#0f172a', fontSize: 12, fontWeight: 'bold',
      },
    }],
  }
})

/* ───────── helpers ───────── */
const currencySymbols = { CNY: '¥', USD: '$', IDR: 'Rp', SGD: 'S$', AUD: 'A$', EUR: '€', GBP: '£', JPY: '¥', MAD: 'MAD', RUB: '₽' }
function fmt(amount, currency = 'CNY') {
  const sym = currencySymbols[currency] || ''
  return `${sym} ${Number(amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}
function label(tx) { return tx.note || tx.description || tx.categoryName || 'Transaction' }
function isIncome(tx) {
  const t = tx.type
  return t === 'INCOME' || t === 'income' || t === 1 || t === '1'
}

const conversionRates = computed(() => {
  if (!rawRates.value) return null
  const rates = {}
  for (const [cur, rate] of Object.entries(rawRates.value)) {
    const num = Number(rate)
    if (num <= 0) continue
    if (!rates['CNY']) rates['CNY'] = {}
    rates['CNY'][cur] = num
    if (!rates[cur]) rates[cur] = {}
    rates[cur]['CNY'] = 1 / num
  }
  const currencies = Object.keys(rates['CNY'] || {})
  for (const c1 of currencies) {
    for (const c2 of currencies) {
      if (c1 === c2) continue
      if (!rates[c1]) rates[c1] = {}
      rates[c1][c2] = rates['CNY'][c2] / rates['CNY'][c1]
    }
  }
  return Object.keys(rates).length > 0 ? rates : null
})

function convertedString(amount, from) {
  if (!conversionRates.value) return null
  if (!from) from = 'CNY'
  if (from === displayCurrency.value) return null
  const rate = conversionRates.value[from]?.[displayCurrency.value]
  if (!rate) return null
  const v = (Number(amount || 0) * rate).toFixed(2)
  return `${currencySymbols[displayCurrency.value] || ''} ${Number(v).toLocaleString('en-US')}`
}

function convertCny(amount) {
  if (!conversionRates.value || displayCurrency.value === 'CNY') return Number(amount || 0)
  const rate = conversionRates.value['CNY']?.[displayCurrency.value]
  if (!rate) return Number(amount || 0)
  return Number(amount || 0) * rate
}

function fmtConverted(amount) {
  const sym = currencySymbols[displayCurrency.value] || ''
  const converted = convertCny(amount)
  return `${sym} ${Number(converted).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}

/* ───────── data fetching ───────── */
const FALLBACK_RATES = { USD: 0.15, IDR: 2654, SGD: 0.19, AUD: 0.21, EUR: 0.13, GBP: 0.11, JPY: 23.99, MAD: 1.38, RUB: 11.54 }

async function fetchRates() {
  ratesError.value = ''
  rawRates.value = { ...FALLBACK_RATES }
  try {
    const data = await getRates('CNY')
    if (data) {
      for (const [k, v] of Object.entries(data)) {
        const num = Number(v)
        if (!isNaN(num) && num > 0) rawRates.value[k] = num
      }
    }
  } catch (err) {
    console.warn('Live rates unavailable, using fallback:', err)
    ratesError.value = 'Using estimated rates'
  }
}

async function fetchStats() {
  try {
    const data = await getStats()
    if (data) {
      const inc = Number(data.income || 0)
      const exp = Number(data.expense || 0)
      stats.value = {
        income: inc, expense: exp, balance: inc - exp,
        transactionCount: data.transactionCount || 0,
        categoryCount: data.categoryCount || categories.value.length,
        budget: data.budget ? Number(data.budget) : null,
        budgetPercent: data.budgetPercent ? Number(data.budgetPercent) : null,
      }
    }
  } catch { /* keep defaults */ }
}

async function fetchData() {
  loading.value = true
  try {
    const [txData, catData] = await Promise.all([
      getTransactions({ page: 1, pageSize: 5 }),
      getCategories(),
    ])
    const txList = txData?.records || txData || []
    categories.value = catData || []
    recentTransactions.value = txList.slice(0, 5)
  } catch {
    // keep defaults
  } finally { loading.value = false }
}

async function fetchChart() {
  chartLoading.value = true
  try {
    const typeVal = chartType.value === 'EXPENSE' ? 2 : 1
    const data = await getByCategory(typeVal)
    chartData.value = data || []
  } catch { chartData.value = [] } finally { chartLoading.value = false }
}

watch(chartType, fetchChart)

/* ───────── dialog ───────── */
function openDialog() { resetForm(); dialogVisible.value = true }
function resetForm() {
  form.value = { amount: null, type: 'INCOME', categoryId: null, note: '', transactionDate: new Date().toISOString().slice(0, 10) }
  formRef.value?.resetFields()
}
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await createTransaction({
      amount: Number(form.value.amount),
      type: form.value.type === 'INCOME' ? 1 : 2,
      categoryId: form.value.categoryId,
      transactionDate: form.value.transactionDate,
      note: form.value.note || null,
    })
    ElMessage.success('Transaction added successfully!')
    dialogVisible.value = false
    await Promise.all([fetchData(), fetchChart(), fetchStats()])
  } catch (err) {
    ElMessage.error(err?.response?.data?.message || 'Could not add transaction. Please try again.')
  } finally { submitting.value = false }
}

function greeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Good Morning'
  if (h < 17) return 'Good Afternoon'
  return 'Good Evening'
}

/* ───────── budget ───────── */
const budgetDialogVisible = ref(false)
const budgetFormRef = ref(null)
const budgetForm = ref({ amount: null })
const budgetSubmitting = ref(false)
const budgetError = ref('')

const budgetRules = {
  amount: [
    { required: true, message: 'Please enter a budget amount', trigger: 'blur' },
    { type: 'number', min: 0.01, message: 'Amount must be greater than 0', trigger: 'blur' },
  ],
}

const budgetPercent = computed(() => {
  return stats.value.budgetPercent ? Number(stats.value.budgetPercent) : 0
})

const budgetAlert = computed(() => {
  const pct = budgetPercent.value
  if (pct >= 100) return { level: 'danger', message: 'Budget exceeded!' }
  if (pct >= 90) return { level: 'warning', message: 'Near budget limit!' }
  return null
})

function openBudgetDialog() {
  budgetForm.value.amount = stats.value.budget ? Number(stats.value.budget) : null
  budgetDialogVisible.value = true
}

async function handleBudgetSubmit() {
  const valid = await budgetFormRef.value.validate().catch(() => false)
  if (!valid) return
  budgetSubmitting.value = true
  budgetError.value = ''
  try {
    await setBudget({ amount: Number(budgetForm.value.amount) })
    ElMessage.success('Budget saved!')
    budgetDialogVisible.value = false
    fetchStats()
  } catch (err) {
    budgetError.value = err?.response?.data?.message || 'Failed to save budget'
  } finally {
    budgetSubmitting.value = false
  }
}

/* ───────── 7-day trend ───────── */
const trendData = ref([])
const trendLoading = ref(false)
const trendStartDate = ref('')
const trendEndDate = ref('')

/* ───────── recent tx filters ───────── */
const txTypeFilter = ref(null)
const txCategoryFilter = ref(null)
const txFilterLabel = ref('')

const filteredRecentTransactions = computed(() => {
  let list = recentTransactions.value
  if (txTypeFilter.value) list = list.filter((t) => isIncome(t) ? txTypeFilter.value === 'INCOME' : txTypeFilter.value === 'EXPENSE')
  if (txCategoryFilter.value) list = list.filter((t) => t.categoryId === txCategoryFilter.value)
  return list
})

const fetchingByCategory = ref(false)

async function chartClick(params) {
  if (!params || !params.name) return
  const cat = categories.value.find((c) => c.name === params.name)
  if (cat) {
    txCategoryFilter.value = cat.id
    txFilterLabel.value = cat.name
    // Fetch ALL transactions for this category from the database
    fetchingByCategory.value = true
    try {
      const data = await getTransactions({ page: 1, pageSize: 50, categoryId: cat.id })
      const txList = data?.records || data || []
      recentTransactions.value = txList
    } catch {
      recentTransactions.value = []
    } finally {
      fetchingByCategory.value = false
    }
  }
}

const filteredCategories = computed(() => {
  if (!txTypeFilter.value) return categories.value
  return categories.value.filter((c) => (c.type === 1 || c.type === 'INCOME') ? txTypeFilter.value === 'INCOME' : txTypeFilter.value === 'EXPENSE')
})

const dialogFilteredCategories = computed(() => {
  const t = form.value.type
  if (!t) return categories.value
  return categories.value.filter((c) => (c.type === 1 || c.type === 'INCOME') ? t === 'INCOME' : t === 'EXPENSE')
})

async function setTypeFilter(type) {
  if (txTypeFilter.value === type) {
    txTypeFilter.value = null
  } else {
    txTypeFilter.value = type
    if (txCategoryFilter.value) {
      const cat = categories.value.find((c) => c.id === txCategoryFilter.value)
      if (cat) {
        const catIsIncome = cat.type === 1 || cat.type === 'INCOME'
        if ((type === 'INCOME' && !catIsIncome) || (type === 'EXPENSE' && catIsIncome)) {
          txCategoryFilter.value = null; txFilterLabel.value = ''
        }
      }
    }
  }
  // Query the database with current filters
  const params = { page: 1, pageSize: 50 }
  if (txTypeFilter.value) params.type = txTypeFilter.value === 'INCOME' ? 1 : 2
  if (txCategoryFilter.value) params.categoryId = txCategoryFilter.value
  try {
    const data = await getTransactions(params)
    const txList = data?.records || data || []
    recentTransactions.value = txList
  } catch { recentTransactions.value = [] }
}

async function setCategoryFilter(catId) {
  if (!catId) {
    txCategoryFilter.value = null
    txFilterLabel.value = ''
    // Reload default recent transactions
    try {
      const data = await getTransactions({ page: 1, pageSize: 5 })
      const txList = data?.records || data || []
      recentTransactions.value = txList.slice(0, 5)
    } catch { recentTransactions.value = [] }
    return
  }
  const cat = categories.value.find((c) => c.id === catId)
  txCategoryFilter.value = catId
  txFilterLabel.value = cat ? cat.name : ''
  // Fetch transactions for this category from the database
  try {
    const data = await getTransactions({ page: 1, pageSize: 50, categoryId: catId })
    const txList = data?.records || data || []
    recentTransactions.value = txList
  } catch { recentTransactions.value = [] }
}

async function clearAllFilters() {
  txTypeFilter.value = null; txCategoryFilter.value = null; txFilterLabel.value = ''
  // Reload the default recent transactions
  try {
    const data = await getTransactions({ page: 1, pageSize: 5 })
    const txList = data?.records || data || []
    recentTransactions.value = txList.slice(0, 5)
  } catch { recentTransactions.value = [] }
}

const trendOption = computed(() => {
  const data = trendData.value
  if (!data || data.length === 0) return {}
  const sym = currencySymbols[displayCurrency.value] || ''
  const rate = conversionRates.value
  const convert = (v) => {
    const num = Number(v || 0)
    if (!rate || displayCurrency.value === 'CNY') return num
    const r = rate.CNY?.[displayCurrency.value]
    return r ? num * r : num
  }
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['Income', 'Expense'], bottom: 0, icon: 'roundRect', itemWidth: 10, itemHeight: 6, textStyle: { fontSize: 11, color: '#64748b' } },
    grid: { left: 8, right: 8, top: 8, bottom: 28, containLabel: true },
    xAxis: { type: 'category', data: data.map((d) => d.date || ''), axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { fontSize: 10, color: '#94a3b8' } },
    yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { fontSize: 10, color: '#94a3b8', formatter: (v) => `${sym} ${v}` } },
    series: [
      { name: 'Income', type: 'line', data: data.map((d) => convert(d.income)), smooth: true, symbol: 'circle', symbolSize: 4, lineStyle: { color: '#16a34a', width: 2 }, itemStyle: { color: '#16a34a' }, areaStyle: { color: 'rgba(22,163,74,0.08)' } },
      { name: 'Expense', type: 'line', data: data.map((d) => convert(d.expense)), smooth: true, symbol: 'circle', symbolSize: 4, lineStyle: { color: '#dc2626', width: 2 }, itemStyle: { color: '#dc2626' }, areaStyle: { color: 'rgba(220,38,38,0.06)' } },
    ],
  }
})

function defaultTrendDates() {
  const end = new Date(); const start = new Date()
  start.setDate(start.getDate() - 7)
  return { start: start.toISOString().slice(0, 10), end: end.toISOString().slice(0, 10) }
}

async function fetchTrend() {
  trendLoading.value = true
  try {
    const s = trendStartDate.value || defaultTrendDates().start
    const e = trendEndDate.value || defaultTrendDates().end
    trendData.value = await getDailyTrend(s, e) || []
  } catch { trendData.value = [] } finally { trendLoading.value = false }
}

onMounted(() => {
  const d = defaultTrendDates()
  trendStartDate.value = d.start; trendEndDate.value = d.end
  openAddTransaction(openDialog)
  fetchRates()
  fetchStats()
  fetchData()
  fetchChart()
  fetchTrend()
})
</script>

<template>
  <div class="space-y-6 animate-fade-in">
    <div class="bg-gradient-to-r from-primary-600 to-primary-700 rounded-2xl p-5 sm:p-6 text-white shadow-md shadow-primary-600/20">
      <div class="flex items-center justify-between flex-wrap gap-3">
        <div>
          <p class="text-primary-100 text-sm font-medium mb-1">{{ greeting() }}</p>
          <h2 class="text-xl sm:text-2xl font-extrabold">Your Financial Overview</h2>
          <p class="text-primary-200 text-sm mt-1 hidden sm:block">Track your income and expenses to stay on top of your money</p>
        </div>
        <el-button @click="openDialog" size="large" class="!bg-white !text-primary-700 !border-none !font-bold !rounded-xl hover:!bg-primary-50 transition-colors">+ New Transaction</el-button>
      </div>
    </div>

    <!-- Budget Alert Banner -->
    <div v-if="budgetAlert" class="rounded-xl px-4 py-3 flex items-center gap-3 text-sm font-semibold" :class="budgetAlert.level === 'danger' ? 'bg-rose-100 text-rose-700 border border-rose-200' : 'bg-amber-100 text-amber-700 border border-amber-200'">
      <svg class="w-5 h-5 shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
      </svg>
      <span>{{ budgetAlert.message }} ({{ budgetPercent }}% of monthly budget used)</span>
    </div>

    <div v-loading="loading" class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      <div class="bg-white rounded-2xl p-5 shadow-card border border-border-light hover:shadow-card-hover transition-shadow duration-200">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 rounded-xl bg-green-100 flex items-center justify-center"><svg class="w-5 h-5 text-income" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg></div>
          <p class="text-2xs text-text-muted uppercase tracking-wider font-semibold">Total Income</p>
        </div>
        <p class="text-xl sm:text-2xl font-extrabold text-income">{{ fmtConverted(stats.income) }}</p>
        <p v-if="displayCurrency !== 'CNY'" class="text-xs text-text-muted mt-0.5">&asymp; {{ fmt(stats.income, 'CNY') }}</p>
      </div>
      <div class="bg-white rounded-2xl p-5 shadow-card border border-border-light hover:shadow-card-hover transition-shadow duration-200">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 rounded-xl bg-rose-100 flex items-center justify-center"><svg class="w-5 h-5 text-expense" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" /></svg></div>
          <p class="text-2xs text-text-muted uppercase tracking-wider font-semibold">Total Expenses</p>
        </div>
        <p class="text-xl sm:text-2xl font-extrabold text-expense">{{ fmtConverted(stats.expense) }}</p>
        <p v-if="displayCurrency !== 'CNY'" class="text-xs text-text-muted mt-0.5">&asymp; {{ fmt(stats.expense, 'CNY') }}</p>
      </div>
      <div class="bg-white rounded-2xl p-5 shadow-card border border-border-light hover:shadow-card-hover transition-shadow duration-200">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 rounded-xl bg-blue-100 flex items-center justify-center"><svg class="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 6l3 1m0 0l-3 9a5.002 5.002 0 006.001 0M6 7l3 9M6 7l6-2m6 2l3-1m-3 1l-3 9a5.002 5.002 0 006.001 0M18 7l3 9m-3-9l-6-2m0-2v2m0 16V5m0 16H9m3 0h3" /></svg></div>
          <p class="text-2xs text-text-muted uppercase tracking-wider font-semibold">Balance</p>
        </div>
        <p class="text-xl sm:text-2xl font-extrabold" :class="convertCny(stats.balance) >= 0 ? 'text-primary-600' : 'text-rose-600'">{{ convertCny(stats.balance) >= 0 ? '+' : '' }}{{ fmtConverted(stats.balance) }}</p>
        <p v-if="displayCurrency !== 'CNY'" class="text-xs text-text-muted mt-0.5">&asymp; {{ stats.balance >= 0 ? '+' : '' }}{{ fmt(stats.balance, 'CNY') }}</p>
      </div>
      <div class="bg-white rounded-2xl p-5 shadow-card border border-border-light hover:shadow-card-hover transition-shadow duration-200">
        <div class="flex items-center gap-3 mb-3">
          <div class="w-10 h-10 rounded-xl bg-purple-100 flex items-center justify-center"><svg class="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" /></svg></div>
          <p class="text-2xs text-text-muted uppercase tracking-wider font-semibold">Activity</p>
        </div>
        <p class="text-xl sm:text-2xl font-extrabold text-text-primary">{{ stats.transactionCount }}</p>
        <p class="text-xs text-text-muted mt-1">{{ stats.categoryCount }} categories used</p>
      </div>
    </div>

    <!-- Monthly Budget Progress -->
    <div class="bg-white rounded-2xl shadow-card border border-border-light p-5">
      <div class="flex items-center justify-between mb-3 flex-wrap gap-2">
        <h3 class="font-bold text-text-primary text-base">Monthly Budget</h3>
        <el-button size="small" @click="openBudgetDialog" class="!rounded-lg">
          {{ stats.budget ? 'Edit Budget' : 'Set Budget' }}
        </el-button>
      </div>
      <div v-if="stats.budget" class="space-y-2">
        <div class="flex justify-between text-sm">
          <span class="text-text-muted">Spent</span>
          <span class="font-semibold text-text-primary">{{ fmtConverted(stats.expense) }} / {{ fmtConverted(stats.budget) }}</span>
        </div>
        <el-progress
          :percentage="budgetPercent > 100 ? 100 : budgetPercent"
          :color="budgetPercent >= 100 ? '#dc2626' : budgetPercent >= 90 ? '#f59e0b' : '#16a34a'"
          :stroke-width="12"
          :show-text="false"
        />
        <div class="flex justify-between text-xs text-text-muted">
          <span>{{ budgetPercent }}% used</span>
          <span>{{ budgetPercent >= 100 ? 'Over budget' : budgetPercent >= 90 ? 'Almost there' : 'On track' }}</span>
        </div>
      </div>
      <div v-else class="text-center py-4 text-text-muted text-sm">
        <p>No budget set for this month.</p>
        <el-button type="primary" size="small" @click="openBudgetDialog" class="!rounded-lg mt-2">Set Monthly Budget</el-button>
      </div>
    </div>

    <div class="bg-white rounded-2xl shadow-card border border-border-light p-5">
      <div class="flex items-center justify-between mb-4 flex-wrap gap-2">
        <h3 class="font-bold text-text-primary text-base">Trend</h3>
        <div class="flex items-center gap-2 flex-wrap">
          <el-date-picker v-model="trendStartDate" type="date" placeholder="Start" size="small" class="w-32" value-format="YYYY-MM-DD" @change="fetchTrend" />
          <span class="text-text-muted text-sm">–</span>
          <el-date-picker v-model="trendEndDate" type="date" placeholder="End" size="small" class="w-32" value-format="YYYY-MM-DD" @change="fetchTrend" />
        </div>
      </div>
      <div v-if="trendData.length > 0" v-loading="trendLoading" style="width: 100%; height: 260px;">
        <v-chart :option="trendOption" style="width:100%;height:100%" autoresize />
      </div>
      <div v-else class="flex items-center justify-center h-[260px] text-text-muted text-sm">Select a date range to view trends</div>
    </div>

    <div class="flex flex-col lg:flex-row gap-5">
      <div class="flex-[3] bg-white rounded-2xl shadow-card border border-border-light overflow-hidden">
        <div class="flex items-center justify-between px-5 py-4 border-b border-border-light">
          <h3 class="font-bold text-text-primary text-base">Recent Transactions</h3>
          <router-link to="/transactions" class="text-sm font-semibold text-primary-600 hover:text-primary-700 no-underline transition-colors">View All &rarr;</router-link>
        </div>
        <div class="px-5 py-3 bg-surface-secondary border-b border-border-light flex items-center gap-3 flex-wrap">
          <span class="text-xs text-text-muted font-semibold uppercase tracking-wider">Filter:</span>
          <div class="flex items-center rounded-lg border border-border-default overflow-hidden bg-white">
            <button @click="setTypeFilter(null)" class="px-3 py-1.5 text-xs font-semibold border-none cursor-pointer transition-colors" :class="!txTypeFilter ? 'bg-primary-600 text-white' : 'text-text-secondary hover:bg-surface-secondary'">All</button>
            <button @click="setTypeFilter('INCOME')" class="px-3 py-1.5 text-xs font-semibold border-none cursor-pointer transition-colors" :class="txTypeFilter === 'INCOME' ? 'bg-income text-white' : 'text-text-secondary hover:bg-surface-secondary'">Income</button>
            <button @click="setTypeFilter('EXPENSE')" class="px-3 py-1.5 text-xs font-semibold border-none cursor-pointer transition-colors" :class="txTypeFilter === 'EXPENSE' ? 'bg-expense text-white' : 'text-text-secondary hover:bg-surface-secondary'">Expense</button>
          </div>
          <el-select v-model="txCategoryFilter" placeholder="Category" clearable size="small" class="w-36" @change="setCategoryFilter">
            <el-option label="All Categories" :value="null" />
            <el-option v-for="cat in filteredCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
          <span v-if="txTypeFilter || txCategoryFilter" class="inline-flex items-center gap-2 ml-2">
            <span class="text-xs text-primary-700 font-medium">{{ txTypeFilter === 'INCOME' ? 'Income' : txTypeFilter === 'EXPENSE' ? 'Expense' : '' }}{{ txTypeFilter && txCategoryFilter ? ' · ' : '' }}{{ txFilterLabel }}</span>
            <el-button size="small" @click="clearAllFilters" class="!rounded-lg" plain>Clear</el-button>
          </span>
        </div>
        <div v-if="!loading && recentTransactions.length === 0" class="flex flex-col items-center justify-center py-12 px-5 text-center">
          <svg class="w-16 h-16 text-text-muted mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4" /></svg>
          <h4 class="font-bold text-text-primary mb-1">No transactions yet</h4>
          <p class="text-text-secondary text-sm mb-4 max-w-xs">Start tracking your money by adding your first income or expense.</p>
          <el-button type="primary" @click="openDialog" class="!rounded-xl">+ Add Your First Transaction</el-button>
        </div>
        <div v-else class="divide-y divide-border-light">
          <div v-for="tx in filteredRecentTransactions" :key="tx.id" class="flex items-center justify-between px-5 py-3.5 hover:bg-surface-secondary transition-colors">
            <div class="flex items-center gap-3 min-w-0">
              <div class="w-9 h-9 rounded-xl flex items-center justify-center shrink-0" :class="isIncome(tx) ? 'bg-green-100' : 'bg-rose-100'">
                <svg v-if="isIncome(tx)" class="w-4 h-4 text-income" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 13l-5 5m0 0l-5-5m5 5V6" /></svg>
                <svg v-else class="w-4 h-4 text-expense" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 11l5-5m0 0l5 5m-5-5v12" /></svg>
              </div>
              <div class="min-w-0"><p class="text-sm font-semibold text-text-primary truncate">{{ label(tx) }}</p><p class="text-xs text-text-muted">{{ tx.transactionDate }}</p></div>
            </div>
            <div class="text-right shrink-0 ml-3">
              <p class="text-sm font-bold" :class="isIncome(tx) ? 'text-income' : 'text-expense'">{{ isIncome(tx) ? '+' : '−' }}{{ fmt(tx.amount, tx.currency || 'CNY') }}</p>
              <p v-if="convertedString(tx.amount, tx.currency || 'CNY')" class="text-xs text-text-muted">&asymp; {{ convertedString(tx.amount, tx.currency || 'CNY') }}</p>
              <p v-else-if="ratesError && (tx.currency || 'CNY') !== displayCurrency" class="text-xs text-warm-500">Rate unavailable</p>
            </div>
          </div>
        </div>
      </div>
      <div class="flex-[2] bg-white rounded-2xl shadow-card border border-border-light p-5 flex flex-col min-h-[300px]">
        <div class="flex items-center justify-between mb-4 flex-wrap gap-2">
          <h3 class="font-bold text-text-primary text-base">By Category</h3>
          <el-radio-group v-model="chartType" size="small">
            <el-radio-button value="EXPENSE">Expenses</el-radio-button>
            <el-radio-button value="INCOME">Income</el-radio-button>
          </el-radio-group>
        </div>
        <div v-loading="chartLoading" class="flex-1 flex items-center justify-center min-h-[220px]">
          <div v-if="chartData.length > 0" style="width: 100%; height: 260px;">
            <v-chart :option="chartOption" style="width:100%;height:100%" autoresize @click="chartClick" />
          </div>
          <div v-else class="text-center">
            <svg class="w-12 h-12 text-text-muted mx-auto mb-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M11 3.055A9.001 9.001 0 1020.945 13H11V3.055z" /><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M20.488 9H15V3.512A9.025 9.025 0 0120.488 9z" /></svg>
            <p class="text-text-secondary text-sm">{{ chartType === 'EXPENSE' ? 'No expense data to show yet' : 'No income data to show yet' }}</p>
            <p class="text-text-muted text-xs mt-1">Add transactions to see breakdown here</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Budget Dialog -->
    <el-dialog v-model="budgetDialogVisible" title="Set Monthly Budget" width="450px" :close-on-click-modal="false" destroy-on-close>
      <el-form ref="budgetFormRef" :model="budgetForm" :rules="budgetRules" label-position="top" @submit.prevent="handleBudgetSubmit">
        <el-form-item label="Budget Amount" prop="amount">
          <el-input v-model.number="budgetForm.amount" placeholder="0.00" type="number" min="0.01" step="0.01" size="large">
            <template #prefix><span class="text-text-muted">¥</span></template>
          </el-input>
        </el-form-item>
        <p v-if="budgetError" class="text-rose-500 text-sm mt-1">{{ budgetError }}</p>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="budgetDialogVisible = false" size="large" class="!rounded-xl">Cancel</el-button>
          <el-button type="primary" :loading="budgetSubmitting" @click="handleBudgetSubmit" size="large" class="!rounded-xl">Save Budget</el-button>
        </div>
      </template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="New Transaction" width="500px" :close-on-click-modal="false" destroy-on-close @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top" @submit.prevent="handleSubmit">
        <el-form-item label="Transaction Type" prop="type">
          <el-radio-group v-model="form.type" class="w-full">
            <el-radio-button value="INCOME" class="flex-1 text-center">Income</el-radio-button>
            <el-radio-button value="EXPENSE" class="flex-1 text-center">Expense</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="Amount" prop="amount">
          <el-input v-model.number="form.amount" placeholder="0.00" type="number" min="0.01" step="0.01" size="large" />
        </el-form-item>
        <el-form-item label="Category" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="Choose a category" class="w-full" size="large">
            <el-option v-for="cat in dialogFilteredCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="Date" prop="transactionDate">
          <el-date-picker v-model="form.transactionDate" type="date" placeholder="Select date" class="w-full" value-format="YYYY-MM-DD" size="large" />
        </el-form-item>
        <el-form-item label="Note (Optional)">
          <el-input v-model="form.note" type="textarea" :rows="2" placeholder="Add a short note about this transaction..." maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="flex justify-end gap-3">
          <el-button @click="dialogVisible = false" size="large" class="!rounded-xl">Cancel</el-button>
          <el-button type="primary" :loading="submitting" @click="handleSubmit" size="large" class="!rounded-xl">Save Transaction</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>
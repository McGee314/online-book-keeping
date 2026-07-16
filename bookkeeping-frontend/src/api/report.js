import request from './request'

/**
 * Fetch per-category aggregated totals for donut chart.
 * @param {number} type - 1 = Income, 2 = Expense (default)
 * @returns {Promise<Array<{categoryName: string, totalAmount: number}>>}
 */
export function getByCategory(type = 2) {
  return request.get('/reports/by-category', { params: { type } })
}

/**
 * Fetch daily income/expense totals for the last 7 days.
 * @returns {Promise<Array<{date: string, income: number, expense: number}>>}
 */
export function getDailyTrend(startDate, endDate) {
  return request.get('/reports/daily-trend', { params: { startDate, endDate } })
}

/**
 * Fetch total income/expense/counts from ALL transactions in database.
 * @returns {Promise<{income: number, expense: number, transactionCount: number, categoryCount: number}>}
 */
export function getStats() {
  return request.get('/reports/stats')
}

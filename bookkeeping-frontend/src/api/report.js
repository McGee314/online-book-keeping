import request from './request'

/**
 * Fetch per-category aggregated totals for donut chart.
 * @param {number} type - 1 = Income, 2 = Expense (default)
 * @returns {Promise<Array<{categoryName: string, totalAmount: number}>>}
 */
export function getByCategory(type = 2) {
  return request.get('/reports/by-category', { params: { type } })
}
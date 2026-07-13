import request from './request'

/**
 * Get exchange rates with the given base currency.
 * Backend returns: { CNY: rate, USD: rate } where rate = 1 base → target.
 * For base=IDR, returns how many CNY/USD per 1 IDR.
 */
export function getRates(base = 'IDR') {
  return request.get('/currency/rates', { params: { base } })
}
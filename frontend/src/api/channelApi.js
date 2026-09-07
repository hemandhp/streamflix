// In dev, Vite proxies /api -> http://localhost:8080 (see vite.config.js), so a relative
// base works both in dev and once the frontend is built and served behind the same host.
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

async function request(path) {
  const res = await fetch(`${BASE_URL}${path}`)
  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body.error || `Request failed: ${res.status}`)
  }
  return res.json()
}

export function getCategories() {
  return request('/api/channels/categories')
}

export function getChannelsByCategory(category, { page = 0, size = 20 } = {}) {
  return request(`/api/channels/category/${encodeURIComponent(category)}?page=${page}&size=${size}`)
}

export function searchChannels(query, { page = 0, size = 40 } = {}) {
  return request(`/api/channels/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`)
}

export function getAllChannels({ page = 0, size = 40 } = {}) {
  return request(`/api/channels?page=${page}&size=${size}`)
}

export function getCatalogStatus() {
  return request('/api/channels/status')
}

/** Builds a same-origin-safe proxied URL for a raw stream URL from the catalog. */
export function proxiedStreamUrl(rawStreamUrl) {
  return `${BASE_URL}/api/proxy?url=${encodeURIComponent(rawStreamUrl)}`
}

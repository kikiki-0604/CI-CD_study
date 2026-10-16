import { Candidate, CookingRecord } from '../types'

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...options
  })
  if (!res.ok) {
    const body = await res.json().catch(() => ({ error: 'unknown error' }))
    throw new Error(body.error ?? `request failed: ${res.status}`)
  }
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

export function fetchSuggestions(ingredients: string[]) {
  return request<{ candidates: Candidate[] }>('/api/suggestions', {
    method: 'POST',
    body: JSON.stringify({ ingredients })
  })
}

export function createRecord(dishName: string, recipeUrl: string, cookedOn: string) {
  return request<{ id: number }>('/api/records', {
    method: 'POST',
    body: JSON.stringify({ dishName, recipeUrl, cookedOn })
  })
}

export function fetchRecords() {
  return request<CookingRecord[]>('/api/records')
}

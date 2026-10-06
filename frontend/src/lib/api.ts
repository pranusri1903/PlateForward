const BASE = import.meta.env.VITE_API_URL ?? ''
const KEY = 'pf_token'

export const token = {
  get: () => localStorage.getItem(KEY),
  set: (t: string) => localStorage.setItem(KEY, t),
  clear: () => localStorage.removeItem(KEY),
}

export class ApiError extends Error {
  status: number
  fields?: Record<string, string>
  constructor(status: number, message: string, fields?: Record<string, string>) {
    super(message)
    this.status = status
    this.fields = fields
  }
}

export async function api<T>(path: string, { method = 'GET', body }: { method?: string; body?: unknown } = {}): Promise<T> {
  const jwt = token.get()
  const res = await fetch(`${BASE}/api${path}`, {
    method,
    headers: { ...(body !== undefined && { 'Content-Type': 'application/json' }), ...(jwt && { Authorization: `Bearer ${jwt}` }) },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (res.status === 204) return undefined as T
  const data = await res.json().catch(() => null)
  if (res.status === 401 && jwt) {
    // The API rejects expired tokens even on public pages, so drop it and retry as a guest.
    token.clear()
    window.dispatchEvent(new Event('pf:logout'))
    return api<T>(path, { method, body })
  }
  if (!res.ok) throw new ApiError(res.status, data?.detail ?? 'Something went wrong. Please try again.', data?.errors)
  return data as T
}

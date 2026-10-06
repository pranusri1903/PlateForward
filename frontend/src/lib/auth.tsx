import { useQueryClient } from '@tanstack/react-query'
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { api, token } from './api'
import type { User } from './types'

interface Session { user: User | null; loading: boolean; signIn: (path: 'login' | 'register', body: object) => Promise<User>; signOut: () => void }
const AuthContext = createContext<Session>(null!)
export const useAuth = () => useContext(AuthContext)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(!!token.get())

  const signOut = useCallback(() => {
    token.clear()
    setUser(null)
    queryClient.clear()
  }, [queryClient])

  useEffect(() => {
    if (token.get()) api<User>('/me').then(setUser).catch(() => token.clear()).finally(() => setLoading(false))
    window.addEventListener('pf:logout', signOut) // fired by the API client when a token expires
    return () => window.removeEventListener('pf:logout', signOut)
  }, [signOut])

  const signIn = useCallback(async (path: 'login' | 'register', body: object) => {
    const res = await api<{ token: string; user: User }>(`/auth/${path}`, { method: 'POST', body })
    token.set(res.token)
    queryClient.clear()
    setUser(res.user)
    return res.user
  }, [queryClient])

  const value = useMemo(() => ({ user, loading, signIn, signOut }), [user, loading, signIn, signOut])
  return <AuthContext value={value}>{children}</AuthContext>
}

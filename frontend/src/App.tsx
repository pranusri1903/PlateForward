import { lazy } from 'react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Route, Routes } from 'react-router'
import { Layout, RequireAuth } from '@/components/Layout'
import { ApiError } from '@/lib/api'
import { AuthProvider } from '@/lib/auth'

const Admin = lazy(() => import('@/pages/Admin'))
const Browse = lazy(() => import('@/pages/Browse'))
const Dashboard = lazy(() => import('@/pages/Dashboard'))
const History = lazy(() => import('@/pages/History'))
const Landing = lazy(() => import('@/pages/Landing'))
const ListingDetail = lazy(() => import('@/pages/ListingDetail'))
const Login = lazy(() => import('@/pages/Login'))
const NewListing = lazy(() => import('@/pages/NewListing'))
const NotFound = lazy(() => import('@/pages/NotFound'))
const Register = lazy(() => import('@/pages/Register'))

const queryClient = new QueryClient({
  defaultOptions: { queries: { staleTime: 15_000, retry: (n, e) => n < 1 && !(e instanceof ApiError && e.status < 500) } },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route element={<Layout />}>
              <Route index element={<Landing />} />
              <Route path="login" element={<Login />} />
              <Route path="register" element={<Register />} />
              <Route path="listings" element={<Browse />} />
              <Route path="listings/:id" element={<ListingDetail />} />
              <Route element={<RequireAuth roles={['DONOR', 'GIVER']} />}><Route path="listings/new" element={<NewListing />} /></Route>
              <Route element={<RequireAuth roles={['DONOR', 'GIVER', 'ORG', 'TAKER']} />}><Route path="dashboard" element={<Dashboard />} /><Route path="history" element={<History />} /></Route>
              <Route element={<RequireAuth roles={['ADMIN']} />}><Route path="admin" element={<Admin />} /></Route>
              <Route path="*" element={<NotFound />} />
            </Route>
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}

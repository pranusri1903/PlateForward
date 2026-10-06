import { Leaf, LogOut, Menu, Plus, X } from 'lucide-react'
import { AnimatePresence, motion } from 'motion/react'
import { Suspense, useState } from 'react'
import { Link, NavLink, Navigate, Outlet, useLocation } from 'react-router'
import { Toaster } from 'sonner'
import { useAuth } from '@/lib/auth'
import type { Role } from '@/lib/types'
import { canGive, cn, homePath } from '@/lib/utils'
import { Button, buttonStyles } from './ui/button'

const navLink = ({ isActive }: { isActive: boolean }) => cn('rounded-full px-4 py-2 text-sm font-medium transition hover:bg-stone-100', isActive ? 'bg-stone-100 text-stone-900' : 'text-stone-600')

function Navbar() {
  const { user, signOut } = useAuth()
  const [open, setOpen] = useState(false)
  const close = () => setOpen(false)
  const links = (
    <>
      <NavLink to="/listings" className={navLink} onClick={close}>Browse food</NavLink>
      {user && <NavLink to={homePath(user)} className={navLink} onClick={close}>{user.role === 'ADMIN' ? 'Admin' : 'Dashboard'}</NavLink>}
    </>
  )
  return (
    <header className="sticky top-0 z-40 border-b border-stone-200/60 bg-white/75 backdrop-blur-xl">
      <nav className="mx-auto flex h-16 max-w-6xl items-center justify-between gap-4 px-5" aria-label="Main">
        <Link to="/" className="flex items-center gap-2.5 font-display text-xl font-extrabold tracking-tight text-stone-900">
          <span className="grid size-9 place-items-center rounded-xl bg-brand-600 text-white shadow-lg shadow-brand-600/30"><Leaf className="size-5" aria-hidden /></span>PlateForward
        </Link>
        <div className="hidden items-center gap-1 md:flex">
          {links}
          {user ? (
            <>
              {canGive(user) && <Link to="/listings/new" className={cn(buttonStyles({ size: 'sm' }), 'ml-2')}><Plus className="size-4" aria-hidden />Post food</Link>}
              <button onClick={signOut} title="Log out" aria-label={`Log out ${user.name}`} className="group ml-2 flex items-center gap-2 rounded-full py-1 pr-3 pl-1 transition hover:bg-stone-100">
                <span className="grid size-8 place-items-center rounded-full bg-brand-100 text-sm font-bold text-brand-700">{user.name[0].toUpperCase()}</span>
                <LogOut className="size-4 text-stone-400 group-hover:text-stone-700" aria-hidden />
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={navLink}>Log in</NavLink>
              <Link to="/register" className={cn(buttonStyles({ size: 'sm' }), 'ml-2')}>Get started</Link>
            </>
          )}
        </div>
        <button className="grid size-10 place-items-center rounded-full hover:bg-stone-100 md:hidden" onClick={() => setOpen(!open)} aria-label="Menu" aria-expanded={open}>
          {open ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
      </nav>
      <AnimatePresence>
        {open && (
          <motion.div initial={{ height: 0, opacity: 0 }} animate={{ height: 'auto', opacity: 1 }} exit={{ height: 0, opacity: 0 }} className="overflow-hidden border-t border-stone-100 md:hidden">
            <div className="flex flex-col gap-1 p-4">
              {links}
              {user ? (
                <>
                  {canGive(user) && <Link to="/listings/new" onClick={close} className={navLink({ isActive: false })}>Post food</Link>}
                  <Button variant="secondary" className="mt-2" onClick={() => { close(); signOut() }}>Log out ({user.name})</Button>
                </>
              ) : (
                <div className="mt-2 flex gap-2"><Link to="/login" onClick={close} className={cn(buttonStyles({ variant: 'secondary' }), 'flex-1')}>Log in</Link><Link to="/register" onClick={close} className={cn(buttonStyles(), 'flex-1')}>Get started</Link></div>
              )}
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  )
}

export function Layout() {
  const { pathname } = useLocation()
  return (
    <div className="flex min-h-screen flex-col">
      <Navbar />
      <motion.main key={pathname} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.3 }} className="mx-auto w-full max-w-6xl flex-1 px-5 py-10">
        <Suspense fallback={<div className="h-96" aria-busy />}><Outlet /></Suspense>
      </motion.main>
      <footer className="py-10 text-center text-sm text-stone-400">Made to keep good food on plates, not in bins. 🌱</footer>
      <Toaster position="top-right" richColors closeButton />
    </div>
  )
}

/** Sends guests to the login page and users without one of the allowed roles to their dashboard. */
export function RequireAuth({ roles }: { roles?: Role[] }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return null
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />
  if (roles && !roles.includes(user.role)) return <Navigate to={homePath(user)} replace />
  return <Outlet />
}

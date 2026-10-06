import { ChevronDown } from 'lucide-react'
import type { ComponentProps, ReactNode } from 'react'
import { cn } from '@/lib/utils'

const control = 'w-full rounded-2xl border border-stone-200 bg-white px-4 py-3 text-stone-900 shadow-xs transition placeholder:text-stone-400 hover:border-stone-300 focus:border-brand-500 focus:ring-4 focus:ring-brand-100 focus:outline-none aria-invalid:border-red-400 aria-invalid:ring-red-100'

export const Input = ({ className, ...p }: ComponentProps<'input'>) => <input className={cn(control, className)} {...p} />
export const Textarea = ({ className, ...p }: ComponentProps<'textarea'>) => <textarea className={cn(control, 'min-h-28 resize-y', className)} {...p} />

export function Select({ className, children, ...p }: ComponentProps<'select'>) {
  return (
    <div className="relative">
      <select className={cn(control, 'cursor-pointer appearance-none pr-10', className)} {...p}>{children}</select>
      <ChevronDown className="pointer-events-none absolute top-1/2 right-4 size-4 -translate-y-1/2 text-stone-400" aria-hidden />
    </div>
  )
}

export function Field({ label, error, hint, className, children }: { label: string; error?: string; hint?: string; className?: string; children: ReactNode }) {
  return (
    <label className={cn('block', className)}>
      <span className="mb-1.5 block text-sm font-semibold text-stone-700">{label}</span>
      {children}
      {error ? <span role="alert" className="mt-1.5 block text-sm text-red-600">{error}</span> : hint && <span className="mt-1.5 block text-xs text-stone-500">{hint}</span>}
    </label>
  )
}

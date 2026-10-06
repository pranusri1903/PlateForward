import type { ReactNode } from 'react'

export const EmptyState = ({ emoji, title, children }: { emoji: string; title: string; children?: ReactNode }) => (
  <div className="col-span-full rounded-3xl border-2 border-dashed border-stone-200 px-6 py-16 text-center">
    <div className="text-5xl" aria-hidden>{emoji}</div>
    <h3 className="mt-4 text-lg font-bold">{title}</h3>
    {children && <div className="mx-auto mt-1 max-w-sm text-stone-500">{children}</div>}
  </div>
)

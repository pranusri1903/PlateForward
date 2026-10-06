import { cva, type VariantProps } from 'class-variance-authority'
import { Loader2 } from 'lucide-react'
import type { ComponentProps } from 'react'
import { cn } from '@/lib/utils'

export const buttonStyles = cva(
  'inline-flex shrink-0 items-center justify-center gap-2 rounded-full font-semibold whitespace-nowrap transition active:scale-[.98] disabled:pointer-events-none disabled:opacity-50',
  {
    variants: {
      variant: {
        primary: 'bg-brand-600 text-white shadow-lg shadow-brand-600/25 hover:-translate-y-px hover:bg-brand-700',
        secondary: 'bg-white text-stone-900 ring-1 ring-stone-200 hover:bg-stone-50',
        ghost: 'text-stone-700 hover:bg-stone-100',
        danger: 'bg-white text-red-600 ring-1 ring-red-200 hover:bg-red-50',
      },
      size: { sm: 'h-9 px-4 text-sm', md: 'h-11 px-6 text-sm', lg: 'h-14 px-8 text-base' },
    },
    defaultVariants: { variant: 'primary', size: 'md' },
  },
)

type Props = ComponentProps<'button'> & VariantProps<typeof buttonStyles> & { loading?: boolean }

export function Button({ variant, size, loading, className, children, disabled, ...props }: Props) {
  return (
    <button className={cn(buttonStyles({ variant, size }), className)} disabled={disabled || loading} {...props}>
      {loading && <Loader2 className="size-4 animate-spin" aria-hidden />}
      {children}
    </button>
  )
}

import { Link } from 'react-router'
import { EmptyState } from '@/components/EmptyState'
import { buttonStyles } from '@/components/ui/button'
import { cn } from '@/lib/utils'

export default function NotFound() {
  return (
    <EmptyState emoji="🧭" title="This page doesn't exist"><Link to="/" className={cn(buttonStyles(), 'mt-4')}>Back home</Link></EmptyState>
  )
}

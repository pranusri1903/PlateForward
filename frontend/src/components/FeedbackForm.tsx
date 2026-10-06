import { useState } from 'react'
import { useSubmitFeedback } from '@/lib/listings'
import type { FeedbackKind } from '@/lib/types'
import { Button } from './ui/button'
import { Select, Textarea } from './ui/field'
import { StarInput } from './Stars'

const LABEL: Record<FeedbackKind, string> = { REVIEW: 'Leave a review', NO_SHOW: 'Report a no-show', ISSUE: 'Report an issue' }

export function FeedbackForm({ listingId, kinds }: { listingId: number; kinds: FeedbackKind[] }) {
  const submit = useSubmitFeedback(listingId)
  const [kind, setKind] = useState(kinds[0])
  const [rating, setRating] = useState(0)
  const [text, setText] = useState('')
  const current = kinds.includes(kind) ? kind : kinds[0] // the review option disappears once it's submitted
  const ready = text.trim() !== '' && (current !== 'REVIEW' || rating > 0)

  return (
    <form className="surface space-y-4 p-6" onSubmit={(e) => {
      e.preventDefault()
      submit.mutate({ kind: current, rating: current === 'REVIEW' ? rating : undefined, text: text.trim() }, { onSuccess: () => { setText(''); setRating(0) } })
    }}>
      <h2 className="text-lg font-bold">{kinds.includes('REVIEW') ? 'How did it go?' : 'Something wrong?'}</h2>
      {kinds.length > 1 && <Select aria-label="Type" value={current} onChange={(e) => setKind(e.target.value as FeedbackKind)}>{kinds.map((k) => <option key={k} value={k}>{LABEL[k]}</option>)}</Select>}
      {current === 'REVIEW' && <StarInput value={rating} onChange={setRating} />}
      <Textarea aria-label="Details" value={text} onChange={(e) => setText(e.target.value)} maxLength={2000} placeholder={current === 'REVIEW' ? 'Share a few words about the pickup…' : 'Tell us what happened…'} />
      <Button variant={current === 'REVIEW' ? 'primary' : 'secondary'} className="w-full" loading={submit.isPending} disabled={!ready}>{current === 'REVIEW' ? 'Post review' : 'Send report'}</Button>
    </form>
  )
}

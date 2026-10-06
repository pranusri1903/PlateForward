import { zodResolver } from '@hookform/resolvers/zod'
import { addHours, format } from 'date-fns'
import { Send } from 'lucide-react'
import { Controller, useForm } from 'react-hook-form'
import { useNavigate } from 'react-router'
import { toast } from 'sonner'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Field, Input, Textarea } from '@/components/ui/field'
import { ApiError } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import { useCreateListing } from '@/lib/listings'
import { CATEGORIES, STORAGES } from '@/lib/meta'
import { cn } from '@/lib/utils'

const when = (v: string) => new Date(v).getTime()
const schema = z.object({
  title: z.string().trim().min(1, 'Required').max(150),
  description: z.string().max(2000).optional(),
  category: z.enum(Object.keys(CATEGORIES) as [keyof typeof CATEGORIES]),
  storage: z.enum(Object.keys(STORAGES) as [keyof typeof STORAGES]),
  quantity: z.string().trim().min(1, 'Required').max(60),
  area: z.string().trim().min(1, 'Required'),
  address: z.string().trim().min(1, 'Required'),
  audience: z.enum(['ORGS', 'ANYONE']),
  pickupStart: z.string().min(1, 'Required'),
  pickupEnd: z.string().min(1, 'Required'),
  expiresAt: z.string().min(1, 'Required'),
}).refine((d) => when(d.pickupStart) < when(d.pickupEnd), { path: ['pickupEnd'], message: 'Must be after pickup opens' })
  .refine((d) => when(d.expiresAt) >= when(d.pickupEnd), { path: ['expiresAt'], message: "Food can't expire before pickup closes" })
type Form = z.infer<typeof schema>

const local = (hours: number) => format(addHours(new Date(), hours), "yyyy-MM-dd'T'HH:mm")

function Choices<T extends string>({ value, onChange, options, label }: { value: T; onChange: (v: T) => void; options: Record<T, { label: string; emoji: string }>; label: string }) {
  return (
    <fieldset>
      <legend className="mb-1.5 text-sm font-semibold text-stone-700">{label}</legend>
      <div className="flex flex-wrap gap-2">
        {(Object.entries(options) as [T, { label: string; emoji: string }][]).map(([k, o]) => (
          <label key={k} className="cursor-pointer">
            <input type="radio" className="peer sr-only" checked={value === k} onChange={() => onChange(k)} />
            <span className={cn('block rounded-full border-2 border-stone-200 px-4 py-2 text-sm font-medium transition hover:border-brand-300 peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-brand-600', value === k && 'border-brand-500 bg-brand-50 text-brand-800')}>{o.emoji} {o.label}</span>
          </label>
        ))}
      </div>
    </fieldset>
  )
}

export default function NewListing() {
  const { user } = useAuth()
  const navigate = useNavigate()
  const create = useCreateListing()
  const { register, control, handleSubmit, setError, formState: { errors } } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: { category: 'PREPARED', storage: 'AMBIENT', audience: user?.role === 'GIVER' ? 'ANYONE' : 'ORGS', area: user?.area ?? '', pickupStart: local(1), pickupEnd: local(3), expiresAt: local(6) },
  })

  const submit = handleSubmit((d) => create.mutate(
    { ...d, description: d.description || null, pickupStart: new Date(d.pickupStart).toISOString(), pickupEnd: new Date(d.pickupEnd).toISOString(), expiresAt: new Date(d.expiresAt).toISOString() },
    {
      onSuccess: (l) => { toast.success('Your listing is live!'); navigate(`/listings/${l.id}`) },
      onError: (e) => {
        if (e instanceof ApiError && e.fields) Object.entries(e.fields).forEach(([k, v]) => setError(k as keyof Form, { message: v }))
        else toast.error(e.message)
      },
    },
  ))

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="text-4xl font-extrabold sm:text-5xl">Share surplus food</h1>
      <p className="mt-2 text-stone-500">Takes about a minute. Organizations can claim it right away.</p>
      <form onSubmit={submit} noValidate className="surface mt-8 space-y-10 p-6 sm:p-9">
        <section className="space-y-5">
          <h2 className="text-lg font-bold">What are you sharing?</h2>
          <Field label="Title" error={errors.title?.message}><Input placeholder="e.g. Fresh sourdough loaves" aria-invalid={!!errors.title} {...register('title')} /></Field>
          <Field label="Quantity" error={errors.quantity?.message}><Input placeholder="e.g. 20 meals, 5 kg" aria-invalid={!!errors.quantity} {...register('quantity')} /></Field>
          <Controller control={control} name="category" render={({ field }) => <Choices label="Category" options={CATEGORIES} value={field.value} onChange={field.onChange} />} />
          <Controller control={control} name="storage" render={({ field }) => <Choices label="Storage requirements" options={STORAGES} value={field.value} onChange={field.onChange} />} />
          <Field label="Notes (optional)" hint="Allergens, packaging, what to bring" error={errors.description?.message}><Textarea {...register('description')} /></Field>
        </section>

        <section className="space-y-5">
          <h2 className="text-lg font-bold">When can it be collected?</h2>
          <div className="grid gap-5 sm:grid-cols-2">
            <Field label="Pickup opens" error={errors.pickupStart?.message}><Input type="datetime-local" aria-invalid={!!errors.pickupStart} {...register('pickupStart')} /></Field>
            <Field label="Pickup closes" error={errors.pickupEnd?.message}><Input type="datetime-local" aria-invalid={!!errors.pickupEnd} {...register('pickupEnd')} /></Field>
          </div>
          <Field label="Food expires at" hint="The listing closes automatically at this time." error={errors.expiresAt?.message}><Input type="datetime-local" aria-invalid={!!errors.expiresAt} {...register('expiresAt')} /></Field>
        </section>

        <section className="space-y-5">
          <h2 className="text-lg font-bold">Where is it?</h2>
          <div className="grid gap-5 sm:grid-cols-2">
            <Field label="Neighborhood" hint="Shown publicly" error={errors.area?.message}><Input aria-invalid={!!errors.area} {...register('area')} /></Field>
            <Field label="Full address" hint="Shared only after you confirm a claim" error={errors.address?.message}><Input autoComplete="street-address" aria-invalid={!!errors.address} {...register('address')} /></Field>
          </div>
          <Controller control={control} name="audience" render={({ field }) => (
            <label className="flex cursor-pointer items-start gap-3 rounded-2xl bg-violet-50 p-4">
              <input type="checkbox" className="mt-1 size-5 accent-violet-600" checked={field.value === 'ANYONE'} onChange={(e) => field.onChange(e.target.checked ? 'ANYONE' : 'ORGS')} />
              <span><span className="block font-semibold text-violet-900">Also open to individuals</span><span className="text-sm text-violet-700">Let neighbors, not just verified organizations, claim this.</span></span>
            </label>
          )} />
        </section>

        <Button size="lg" className="w-full" loading={create.isPending}><Send className="size-5" aria-hidden />Publish listing</Button>
      </form>
    </div>
  )
}

import { zodResolver } from '@hookform/resolvers/zod'
import { Controller, useForm } from 'react-hook-form'
import { Link, Navigate, useNavigate, useSearchParams } from 'react-router'
import { toast } from 'sonner'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Field, Input } from '@/components/ui/field'
import { ApiError } from '@/lib/api'
import { useAuth } from '@/lib/auth'
import { SIGNUP_ROLES } from '@/lib/meta'
import { cn } from '@/lib/utils'

const schema = z.object({
  role: z.enum(['DONOR', 'ORG', 'GIVER', 'TAKER']),
  name: z.string().trim().min(1, 'Required'),
  area: z.string().trim().min(1, 'Required'),
  email: z.email('Enter a valid email'),
  phone: z.string().optional(),
  password: z.string().min(8, 'Use at least 8 characters'),
})
type Form = z.infer<typeof schema>

export default function Register() {
  const { user, signIn } = useAuth()
  const navigate = useNavigate()
  const initial = useSearchParams()[0].get('role') as Form['role']
  const { register, control, watch, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm<Form>({
    resolver: zodResolver(schema),
    defaultValues: { role: initial in SIGNUP_ROLES ? initial : 'DONOR' },
  })
  const role = watch('role')

  if (user) return <Navigate to="/dashboard" replace />
  const submit = handleSubmit(async (data) => {
    try {
      const created = await signIn('register', data)
      toast.success(created.role === 'ORG' ? 'Welcome! Our team will verify your organization shortly.' : 'Welcome to PlateForward!')
      navigate('/dashboard', { replace: true })
    } catch (e) {
      if (!(e instanceof ApiError)) return toast.error('Could not reach the server')
      if (e.fields) Object.entries(e.fields).forEach(([k, v]) => setError(k as keyof Form, { message: v }))
      else setError(e.status === 409 ? 'email' : 'root', { message: e.message })
    }
  })

  return (
    <div className="surface mx-auto max-w-2xl p-8 sm:p-10">
      <h1 className="text-3xl font-extrabold">Create your account</h1>
      <p className="mt-1.5 text-stone-500">Choose how you'd like to use PlateForward.</p>
      <form onSubmit={submit} className="mt-8 space-y-7" noValidate>
        <Controller control={control} name="role" render={({ field }) => (
          <fieldset className="grid gap-3 sm:grid-cols-2">
            <legend className="sr-only">Account type</legend>
            {Object.entries(SIGNUP_ROLES).map(([key, { label, blurb, icon: Icon }]) => (
              <label key={key} className="cursor-pointer">
                <input type="radio" className="peer sr-only" checked={field.value === key} onChange={() => field.onChange(key)} />
                <div className={cn('h-full rounded-2xl border-2 border-stone-200 p-4 transition hover:border-brand-300 peer-focus-visible:outline-2 peer-focus-visible:outline-offset-2 peer-focus-visible:outline-brand-600', field.value === key && 'border-brand-500 bg-brand-50 shadow-soft')}>
                  <Icon className="size-6 text-brand-600" aria-hidden />
                  <div className="mt-2 font-bold text-stone-900">{label}</div>
                  <div className="text-sm text-stone-500">{blurb}</div>
                </div>
              </label>
            ))}
          </fieldset>
        )} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label={role === 'ORG' || role === 'DONOR' ? 'Organization name' : 'Your name'} error={errors.name?.message}><Input autoComplete="name" aria-invalid={!!errors.name} {...register('name')} /></Field>
          <Field label="Neighborhood or city" error={errors.area?.message}><Input aria-invalid={!!errors.area} {...register('area')} /></Field>
          <Field label="Email" error={errors.email?.message}><Input type="email" autoComplete="email" aria-invalid={!!errors.email} {...register('email')} /></Field>
          <Field label="Phone (optional)" hint="Shared only after a pickup is confirmed"><Input type="tel" autoComplete="tel" {...register('phone')} /></Field>
          <Field label="Password" error={errors.password?.message} className="sm:col-span-2"><Input type="password" autoComplete="new-password" aria-invalid={!!errors.password} {...register('password')} /></Field>
        </div>
        {role === 'ORG' && <p className="rounded-2xl bg-amber-50 px-4 py-3 text-sm text-amber-800">Organizations are verified by our team before they can claim food. You can browse right away.</p>}
        {errors.root && <p role="alert" className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{errors.root.message}</p>}
        <Button size="lg" className="w-full" loading={isSubmitting}>Create account</Button>
        <p className="text-center text-sm text-stone-500">Already have an account? <Link to="/login" className="font-semibold text-brand-700 hover:underline">Log in</Link></p>
      </form>
    </div>
  )
}

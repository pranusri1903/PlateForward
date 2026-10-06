import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Field, Input } from '@/components/ui/field'
import { useAuth } from '@/lib/auth'
import { homePath } from '@/lib/utils'

const schema = z.object({ email: z.email('Enter a valid email'), password: z.string().min(1, 'Enter your password') })

export default function Login() {
  const { user, signIn } = useAuth()
  const navigate = useNavigate()
  const from = (useLocation().state as { from?: string } | null)?.from
  const { register, handleSubmit, setError, formState: { errors, isSubmitting } } = useForm({ resolver: zodResolver(schema) })

  if (user) return <Navigate to={from ?? homePath(user)} replace />
  const submit = handleSubmit(async (data) => {
    try {
      const signedIn = await signIn('login', data)
      navigate(from ?? homePath(signedIn), { replace: true })
    } catch (e) {
      setError('root', { message: (e as Error).message })
    }
  })

  return (
    <div className="surface mx-auto max-w-md p-8 sm:p-10">
      <h1 className="text-3xl font-extrabold">Welcome back</h1>
      <p className="mt-1.5 text-stone-500">Log in to manage your pickups.</p>
      <form onSubmit={submit} className="mt-8 space-y-4" noValidate>
        <Field label="Email" error={errors.email?.message}><Input type="email" autoComplete="email" aria-invalid={!!errors.email} {...register('email')} /></Field>
        <Field label="Password" error={errors.password?.message}><Input type="password" autoComplete="current-password" aria-invalid={!!errors.password} {...register('password')} /></Field>
        {errors.root && <p role="alert" className="rounded-2xl bg-red-50 px-4 py-3 text-sm text-red-700">{errors.root.message}</p>}
        <Button size="lg" className="w-full" loading={isSubmitting}>Log in</Button>
      </form>
      <p className="mt-6 text-center text-sm text-stone-500">New here? <Link to="/register" className="font-semibold text-brand-700 hover:underline">Create an account</Link></p>
    </div>
  )
}

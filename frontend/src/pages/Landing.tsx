import { ArrowRight, BadgeCheck, HandHelping, PackageCheck, PencilLine, Sparkles } from 'lucide-react'
import { motion } from 'motion/react'
import { Link } from 'react-router'
import { usePageTitle } from '@/components/hooks'
import { ListingCard } from '@/components/ListingCard'
import { buttonStyles } from '@/components/ui/button'
import { useAuth } from '@/lib/auth'
import { useBrowse } from '@/lib/listings'
import { SIGNUP_ROLES } from '@/lib/meta'
import { cn, homePath } from '@/lib/utils'

const STEPS = [
  { icon: PencilLine, title: 'List', text: 'Post the quantity, pickup window, storage needs and expiry in under a minute.' },
  { icon: HandHelping, title: 'Claim', text: 'A verified organization claims it. Only one claim can hold a listing at a time.' },
  { icon: BadgeCheck, title: 'Confirm', text: 'The donor confirms, and only then is the pickup address shared.' },
  { icon: PackageCheck, title: 'Collect', text: 'Reminders keep everyone on time. Then rate the pickup.' },
]
const params = new URLSearchParams({ size: '3' })
const fadeUp = { initial: { opacity: 0, y: 24 }, whileInView: { opacity: 1, y: 0 }, viewport: { once: true, margin: '-60px' }, transition: { duration: 0.5 } }

export default function Landing() {
  usePageTitle()
  const { user } = useAuth()
  const { data } = useBrowse(params)
  const live = data?.content ?? []
  return (
    <>
      <section className="relative grid items-center gap-14 py-6 lg:grid-cols-[1.05fr_1fr] lg:py-14">
        <div className="pointer-events-none absolute -top-24 -left-32 -z-10 size-[28rem] rounded-full bg-brand-200/50 blur-3xl" aria-hidden />
        <div className="pointer-events-none absolute top-20 right-0 -z-10 size-96 rounded-full bg-lime-200/40 blur-3xl" aria-hidden />
        <motion.div initial={{ opacity: 0, y: 24 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.6 }}>
          <span className="inline-flex items-center gap-2 rounded-full bg-brand-100 px-4 py-1.5 text-sm font-semibold text-brand-700"><Sparkles className="size-4" aria-hidden />No more phone tag. No more waste.</span>
          <h1 className="mt-6 text-5xl leading-[1.02] font-extrabold sm:text-7xl">Good food deserves a <span className="bg-gradient-to-r from-brand-600 to-lime-500 bg-clip-text text-transparent">second plate.</span></h1>
          <p className="mt-6 max-w-xl text-lg text-stone-600">Restaurants, grocers and neighbors post surplus food in seconds. Verified community organizations claim it, collect it and feed people, before it expires.</p>
          <div className="mt-9 flex flex-wrap gap-3">
            <Link to={user ? homePath(user) : '/register'} className={buttonStyles({ size: 'lg' })}>{user ? 'Go to dashboard' : 'Get started free'}<ArrowRight className="size-5" aria-hidden /></Link>
            <Link to="/listings" className={buttonStyles({ size: 'lg', variant: 'secondary' })}>Browse available food</Link>
          </div>
          <p className="mt-8 text-sm font-medium text-stone-500"><span className="mr-2 inline-block size-2 animate-pulse rounded-full bg-brand-500" aria-hidden />{data ? `${data.page.totalElements} listing${data.page.totalElements === 1 ? '' : 's'} available right now` : 'Loading live listings…'}</p>
        </motion.div>

        <div className="relative hidden lg:block">
          <div className="absolute -inset-8 -z-10 rounded-[3rem] bg-gradient-to-br from-brand-100 to-lime-100 opacity-70 blur-2xl" aria-hidden />
          <div className="space-y-4">
            {live.length ? live.map((l, i) => <div key={l.id} className={cn(i === 1 && 'translate-x-8', i === 0 && 'rotate-[-1.5deg]', i === 2 && 'rotate-1')}><ListingCard listing={l} index={i} /></div>) : (
              <div className="surface p-12 text-center text-stone-500"><div className="text-5xl">🌱</div><p className="mt-3 font-medium">Be the first to share surplus food.</p></div>
            )}
          </div>
        </div>
      </section>

      <section className="py-20">
        <motion.div {...fadeUp} className="mx-auto max-w-2xl text-center">
          <h2 className="text-4xl font-extrabold">From listing to pickup in four steps</h2>
          <p className="mt-3 text-stone-600">Everyone sees exactly where a pickup stands, from the first post to the final handoff.</p>
        </motion.div>
        <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {STEPS.map(({ icon: Icon, title, text }, i) => (
            <motion.div key={title} {...fadeUp} transition={{ duration: 0.5, delay: i * 0.08 }} className="surface relative overflow-hidden p-6">
              <span className="absolute -top-3 right-3 font-display text-8xl font-black text-stone-100 select-none" aria-hidden>{i + 1}</span>
              <span className="relative grid size-12 place-items-center rounded-2xl bg-brand-100 text-brand-700"><Icon className="size-6" aria-hidden /></span>
              <h3 className="relative mt-5 text-xl font-bold">{title}</h3>
              <p className="relative mt-1.5 text-sm text-stone-600">{text}</p>
            </motion.div>
          ))}
        </div>
      </section>

      <section className="py-4">
        <motion.h2 {...fadeUp} className="text-center text-4xl font-extrabold">There's a place for everyone</motion.h2>
        <div className="mt-12 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {Object.entries(SIGNUP_ROLES).map(([role, { label, blurb, icon: Icon }], i) => (
            <motion.div key={role} {...fadeUp} transition={{ duration: 0.5, delay: i * 0.08 }}>
              <Link to={`/register?role=${role}`} className="group block h-full rounded-3xl bg-stone-900 p-6 text-white transition duration-300 hover:-translate-y-1 hover:bg-brand-700 hover:shadow-lift">
                <Icon className="size-8 text-brand-300 transition group-hover:text-white" aria-hidden />
                <h3 className="mt-5 text-xl font-bold text-white">{label}</h3>
                <p className="mt-1.5 text-sm text-stone-300">{blurb}</p>
                <span className="mt-5 inline-flex items-center gap-1 text-sm font-semibold text-brand-300 group-hover:text-white">Join<ArrowRight className="size-4 transition group-hover:translate-x-1" aria-hidden /></span>
              </Link>
            </motion.div>
          ))}
        </div>
      </section>
    </>
  )
}

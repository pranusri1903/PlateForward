import { Building2, HandHeart, HeartHandshake, House, type LucideIcon } from 'lucide-react'
import type { Category, Role, Status, Storage } from './types'

export const CATEGORIES: Record<Category, { label: string; emoji: string; tint: string }> = {
  PRODUCE: { label: 'Produce', emoji: '🥬', tint: 'from-lime-100 to-emerald-100' },
  BAKERY: { label: 'Bakery', emoji: '🥖', tint: 'from-amber-100 to-orange-100' },
  PREPARED: { label: 'Prepared meals', emoji: '🍲', tint: 'from-orange-100 to-rose-100' },
  DAIRY: { label: 'Dairy', emoji: '🧀', tint: 'from-yellow-100 to-amber-100' },
  MEAT: { label: 'Meat & fish', emoji: '🍗', tint: 'from-rose-100 to-red-100' },
  PANTRY: { label: 'Pantry', emoji: '🥫', tint: 'from-stone-100 to-amber-100' },
  OTHER: { label: 'Other', emoji: '🍽️', tint: 'from-sky-100 to-indigo-100' },
}

export const STORAGES: Record<Storage, { label: string; emoji: string }> = {
  AMBIENT: { label: 'Room temp', emoji: '🏠' },
  REFRIGERATED: { label: 'Refrigerated', emoji: '❄️' },
  FROZEN: { label: 'Frozen', emoji: '🧊' },
  HOT: { label: 'Keep hot', emoji: '🔥' },
}

export const STATUSES: Record<Status, { label: string; className: string }> = {
  LISTED: { label: 'Available', className: 'bg-brand-100 text-brand-700' },
  CLAIMED: { label: 'Awaiting confirmation', className: 'bg-amber-100 text-amber-800' },
  CONFIRMED: { label: 'Confirmed', className: 'bg-sky-100 text-sky-800' },
  PICKED_UP: { label: 'Picked up', className: 'bg-stone-800 text-white' },
  EXPIRED: { label: 'Expired', className: 'bg-red-100 text-red-700' },
}

export const SIGNUP_ROLES: Record<Exclude<Role, 'ADMIN'>, { label: string; blurb: string; icon: LucideIcon }> = {
  DONOR: { label: 'Business', blurb: 'Restaurant, grocer or caterer with surplus food', icon: Building2 },
  ORG: { label: 'Organization', blurb: 'Food bank, shelter or charity collecting food', icon: HeartHandshake },
  GIVER: { label: 'Sharing at home', blurb: 'I have extra food at home to give away', icon: House },
  TAKER: { label: 'Looking for food', blurb: "I'd like to collect food shared nearby", icon: HandHeart },
}

export const ROLE_LABEL: Record<Role, string> = { ...Object.fromEntries(Object.entries(SIGNUP_ROLES).map(([k, v]) => [k, v.label])), ADMIN: 'Admin' } as Record<Role, string>

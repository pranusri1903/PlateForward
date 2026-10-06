import { clsx, type ClassValue } from 'clsx'
import { format } from 'date-fns'
import { twMerge } from 'tailwind-merge'
import type { Listing, User } from './types'

export const cn = (...inputs: ClassValue[]) => twMerge(clsx(inputs))

export const fmtDateTime = (iso: string) => format(new Date(iso), 'EEE, MMM d · h:mm a')
export const fmtWindow = (l: Pick<Listing, 'pickupStart' | 'pickupEnd'>) => `${fmtDateTime(l.pickupStart)} – ${format(new Date(l.pickupEnd), 'h:mm a')}`

export const canGive = (u?: User | null) => u?.role === 'DONOR' || u?.role === 'GIVER'

/** Where a user lands after logging in. */
export const homePath = (u?: User | null) => (u?.role === 'ADMIN' ? '/admin' : '/dashboard')

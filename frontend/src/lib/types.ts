export type Role = 'DONOR' | 'ORG' | 'GIVER' | 'TAKER' | 'ADMIN'
export type Status = 'LISTED' | 'CLAIMED' | 'CONFIRMED' | 'PICKED_UP' | 'EXPIRED'
export type Category = 'PRODUCE' | 'BAKERY' | 'PREPARED' | 'DAIRY' | 'MEAT' | 'PANTRY' | 'OTHER'
export type Storage = 'AMBIENT' | 'REFRIGERATED' | 'FROZEN' | 'HOT'
export type Audience = 'ORGS' | 'ANYONE'
export type Action = 'claim' | 'confirm' | 'decline' | 'release' | 'pickup' | 'delete'

export interface User {
  id: number
  email: string
  name: string
  role: Role
  phone: string | null
  area: string | null
  verified: boolean
}

export interface Party { id: number; name: string; phone: string | null }

export interface Listing {
  id: number
  title: string
  description: string | null
  category: Category
  storage: Storage
  quantity: string
  area: string
  address: string | null // hidden by the API until the pickup is confirmed
  audience: Audience
  status: Status
  pickupStart: string
  pickupEnd: string
  expiresAt: string
  claimExpiresAt: string | null
  pickedUpAt: string | null
  createdAt: string
  donor: Party
  claimer: Party | null
  actions: Action[] // what the current viewer may do next, decided by the backend
}

export interface Page<T> {
  content: T[]
  page: { size: number; number: number; totalElements: number; totalPages: number }
}

export type NewListing = Pick<Listing, 'title' | 'description' | 'category' | 'storage' | 'quantity' | 'area' | 'address' | 'audience' | 'pickupStart' | 'pickupEnd' | 'expiresAt'>

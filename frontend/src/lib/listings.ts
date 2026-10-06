import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { api, ApiError } from './api'
import type { Action, FeedbackKind, Listing, NewListing, Page } from './types'

/** Everything on screen refreshes every 15s (while the tab is visible), so claims and confirmations show up without a reload. */
const POLL = 15_000

export const useListing = (id: string | undefined) => useQuery({ queryKey: ['listing', id], queryFn: () => api<Listing>(`/listings/${id}`), enabled: !!id, refetchInterval: POLL })

export const useBrowse = (params: URLSearchParams) =>
  useQuery({ queryKey: ['listings', params.toString()], queryFn: () => api<Page<Listing>>(`/listings?${params}`), placeholderData: keepPreviousData, refetchInterval: POLL })

export const useMine = () => useQuery({ queryKey: ['mine'], queryFn: () => api<Listing[]>('/listings/mine'), refetchInterval: POLL })

export const useHistory = () => useQuery({ queryKey: ['history'], queryFn: () => api<Listing[]>('/listings/history') })

export const useCreateListing = () => useMutation({ mutationFn: (body: NewListing) => api<Listing>('/listings', { method: 'POST', body }) })

export function useSubmitFeedback(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (body: { kind: FeedbackKind; rating?: number; text: string }) => api(`/listings/${id}/feedback`, { method: 'POST', body }),
    onSuccess: (_, { kind }) => {
      queryClient.invalidateQueries({ queryKey: ['listing', String(id)] })
      toast.success(kind === 'REVIEW' ? 'Thanks for the review!' : 'Report sent. Our team will take a look.')
    },
    onError: (e) => toast.error(e.message),
  })
}

/** Runs one workflow step. The server decides if it is allowed; on a conflict we just refresh to show the latest state. */
export function useListingAction(id: number, onDeleted: () => void) {
  const queryClient = useQueryClient()
  return useMutation<Listing | undefined, Error, Action>({
    mutationFn: (action) => action === 'delete' ? api<undefined>(`/listings/${id}`, { method: 'DELETE' }) : api<Listing>(`/listings/${id}/${action}`, { method: 'POST' }),
    onSuccess: (listing, action) => {
      if (action === 'delete') onDeleted()
      else queryClient.setQueryData(['listing', String(id)], listing)
      queryClient.invalidateQueries({ queryKey: ['listings'] })
      queryClient.invalidateQueries({ queryKey: ['mine'] })
      toast.success(SUCCESS[action])
    },
    onError: (e) => {
      toast.error(e.message)
      if (e instanceof ApiError && e.status === 409) queryClient.invalidateQueries({ queryKey: ['listing', String(id)] })
    },
  })
}

const SUCCESS: Record<Action, string> = {
  claim: 'Claimed! The donor has 2 hours to confirm.',
  confirm: 'Confirmed. The pickup address is now shared.',
  decline: 'Claim declined. The listing is back on the board.',
  release: 'Claim released. The listing is available again.',
  pickup: 'Marked as picked up. Thank you for reducing waste! 🎉',
  delete: 'Listing removed.',
}

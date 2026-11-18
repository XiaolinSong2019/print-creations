// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, expect, it, vi } from 'vitest'
import AdminRushApprovalPage from './AdminRushApprovalPage'

const referenceNumber = '51afdb79-7779-40ac-85ac-2f1722542a79'
const quotePath = `/api/admin/quotes/${referenceNumber}`
const pendingQuote = {
    referenceNumber,
    firstName: 'Test',
    lastName: 'Customer',
    quantity: 250,
    fulfillmentMethod: 'PICKUP',
    status: 'NEW',
    rushRequested: true,
    rushApproved: false,
    rushFeeAmount: null,
    rushRequestedCompletionDate: null,
    rushNotes: 'Need a Friday pickup.',
    version: 0,
}
const approvedQuote = { ...pendingQuote, rushApproved: true, rushFeeAmount: 25.5, version: 1 }
const csrf = { headerName: 'X-CSRF-TOKEN', token: 'test-token' }

function renderPage(fetchMock: typeof fetch) {
    vi.stubGlobal('fetch', fetchMock)
    render(<MemoryRouter><AdminRushApprovalPage /></MemoryRouter>)
    fireEvent.change(screen.getByLabelText('Quote reference'), { target: { value: referenceNumber } })
    fireEvent.change(screen.getByLabelText('Local Admin password'), { target: { value: 'local-test-password' } })
    fireEvent.click(screen.getByRole('button', { name: 'Find quote' }))
}

afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
})

it('looks up a quote and sends one approval with the current version after confirmation', async () => {
    const fetchMock = vi.fn<typeof fetch>()
        .mockResolvedValueOnce(Response.json(pendingQuote))
        .mockResolvedValueOnce(Response.json(csrf))
        .mockResolvedValueOnce(Response.json(approvedQuote))
    renderPage(fetchMock)

    await screen.findByRole('heading', { name: 'Quote details' })
    const fee = screen.getByLabelText('Rush fee (USD)')
    fireEvent.change(fee, { target: { value: '25.555' } })
    fireEvent.click(screen.getByRole('button', { name: 'Review approval' }))
    expect(screen.getByRole('alert').textContent).toContain('two decimal places')
    expect(fetchMock).toHaveBeenCalledTimes(1)

    fireEvent.change(fee, { target: { value: '25.50' } })
    fireEvent.click(screen.getByRole('button', { name: 'Review approval' }))
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(fetchMock).toHaveBeenCalledTimes(1)

    fireEvent.click(screen.getByRole('button', { name: 'Review approval' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirm approval' }))
    await screen.findByText('Rush approval saved.')
    expect(screen.getByText('$25.50').textContent).toBe('$25.50')
    expect(screen.getByText('NEW').textContent).toBe('NEW')

    expect(fetchMock.mock.calls.map(([path]) => path)).toEqual([
        quotePath, '/api/admin/csrf', `${quotePath}/rush-approval`,
    ])
    const request = fetchMock.mock.calls[2]?.[1]
    expect(request?.method).toBe('PATCH')
    expect(request?.credentials).toBe('same-origin')
    expect(new Headers(request?.headers).get('X-CSRF-TOKEN')).toBe('test-token')
    expect(new Headers(request?.headers).get('Authorization')).toMatch(/^Basic /)
    expect(JSON.parse(String(request?.body))).toEqual({
        rushApproved: true, rushFeeAmount: 25.5, expectedVersion: 0,
    })
})

it('revokes the existing approval and clears its fee', async () => {
    const fetchMock = vi.fn<typeof fetch>()
        .mockResolvedValueOnce(Response.json(approvedQuote))
        .mockResolvedValueOnce(Response.json(csrf))
        .mockResolvedValueOnce(Response.json({ ...pendingQuote, version: 2 }))
    renderPage(fetchMock)

    fireEvent.click(await screen.findByRole('button', { name: 'Review revocation' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirm revocation' }))
    await screen.findByText('Rush approval revoked.')
    expect(screen.queryByText('$25.50')).toBeNull()
    expect(JSON.parse(String(fetchMock.mock.calls[2]?.[1]?.body))).toEqual({
        rushApproved: false, rushFeeAmount: null, expectedVersion: 1,
    })
})

it('requires reloading after a conflict and does not retry the write', async () => {
    const fetchMock = vi.fn<typeof fetch>()
        .mockResolvedValueOnce(Response.json(pendingQuote))
        .mockResolvedValueOnce(Response.json(csrf))
        .mockResolvedValueOnce(new Response(null, { status: 409 }))
        .mockResolvedValueOnce(Response.json(approvedQuote))
    renderPage(fetchMock)

    await screen.findByRole('heading', { name: 'Quote details' })
    fireEvent.change(screen.getByLabelText('Rush fee (USD)'), { target: { value: '30.00' } })
    fireEvent.click(screen.getByRole('button', { name: 'Review approval' }))
    fireEvent.click(screen.getByRole('button', { name: 'Confirm approval' }))
    expect((await screen.findByRole('alert')).textContent).toContain('Reload it')
    expect(screen.queryByRole('button', { name: 'Review approval' })).toBeNull()
    expect(fetchMock).toHaveBeenCalledTimes(3)

    fireEvent.click(screen.getByRole('button', { name: 'Reload quote' }))
    await screen.findByRole('button', { name: 'Review revocation' })
    expect(fetchMock.mock.calls[3]?.[0]).toBe(quotePath)
    expect(fetchMock.mock.calls.filter(([, request]) => request?.method === 'PATCH')).toHaveLength(1)
})

it('does not reveal quote details when the server rejects authentication', async () => {
    const fetchMock = vi.fn<typeof fetch>().mockResolvedValueOnce(new Response(null, { status: 401 }))
    renderPage(fetchMock)

    expect((await screen.findByRole('alert')).textContent).toContain('authentication failed')
    expect(screen.queryByRole('heading', { name: 'Quote details' })).toBeNull()
    expect((screen.getByLabelText('Local Admin password') as HTMLInputElement).value).toBe('')
})

it('does not offer an approval for a quote without a rush request', async () => {
    const fetchMock = vi.fn<typeof fetch>()
        .mockResolvedValueOnce(Response.json({ ...pendingQuote, rushRequested: false }))
    renderPage(fetchMock)

    await screen.findByText('Not requested')
    expect(screen.queryByRole('button', { name: 'Review approval' })).toBeNull()
    expect(fetchMock).toHaveBeenCalledTimes(1)
})

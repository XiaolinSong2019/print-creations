// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
import { afterEach, expect, it, vi } from 'vitest'
import App from './App'

function renderProduct(quoteResponse: () => Response) {
    const fetchMock = vi.fn(async (path: string, options?: RequestInit) => {
        if (path === '/api/categories') {
            return Response.json([{ id: 2, name: 'Stationery & Forms' }])
        }

        if (path === '/api/products') {
            return Response.json([{ id: 12, name: 'Business Cards', categoryId: 2 }])
        }

        if (path === '/api/quote-requests' && options?.method === 'POST') {
            return quoteResponse()
        }

        throw new Error(`Unexpected request: ${path}`)
    })

    vi.stubGlobal('fetch', fetchMock)
    render(
        <MemoryRouter initialEntries={['/products/12']}>
            <App />
        </MemoryRouter>,
    )

    return fetchMock
}

async function fillQuoteForm() {
    fireEvent.click(await screen.findByRole('link', { name: 'Request a Quote' }))
    await screen.findByRole('heading', { name: 'Request a Quote' })

    const fields = {
        'First name': 'Test',
        'Last name': 'Customer',
        Phone: '415-555-0100',
        Email: 'quote-test@example.com',
        Quantity: '250',
        'Fulfillment method': 'PICKUP',
    }

    for (const [label, value] of Object.entries(fields)) {
        fireEvent.change(screen.getByLabelText(label), { target: { value } })
    }
}

afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
})

it('submits a quote for the selected product and shows the saved reference', async () => {
    const referenceNumber = 'eeb8c4cd-15cf-4118-aaf7-86452c45b8ab'
    const fetchMock = renderProduct(() =>
        Response.json({ referenceNumber, status: 'NEW' }, { status: 201 }),
    )
    await fillQuoteForm()
    fireEvent.click(screen.getByRole('checkbox', { name: 'Request rush service' }))
    fireEvent.change(screen.getByLabelText('Requested rush completion date (optional)'), {
        target: { value: '2026-12-04' },
    })
    fireEvent.change(screen.getByLabelText('Rush notes (optional)'), {
        target: { value: 'Please confirm rush availability.' },
    })

    fireEvent.click(screen.getByRole('button', { name: 'Submit quote request' }))

    await screen.findByRole('heading', { name: 'Quote request received' })
    expect(screen.getByRole('status').textContent).toContain(referenceNumber)

    const post = fetchMock.mock.calls.find(([path]) => path === '/api/quote-requests')
    expect(JSON.parse(String(post?.[1]?.body))).toMatchObject({
        productId: 12,
        quantity: 250,
        fulfillmentMethod: 'PICKUP',
        rushRequested: true,
        rushRequestedCompletionDate: '2026-12-04',
        rushNotes: 'Please confirm rush availability.',
        marketingOptIn: false,
    })
})

it('shows the server validation message and keeps the entered details', async () => {
    renderProduct(() => Response.json({
        detail: 'Some fields are invalid.',
        errors: { phone: 'Enter a valid phone number.' },
    }, { status: 400 }))
    await fillQuoteForm()

    const phone = screen.getByLabelText<HTMLInputElement>('Phone')
    fireEvent.change(phone, { target: { value: 'abc' } })
    fireEvent.click(screen.getByRole('button', { name: 'Submit quote request' }))

    expect((await screen.findByRole('alert')).textContent)
        .toContain('Phone: Enter a valid phone number.')
    expect(phone.value).toBe('abc')
    expect(screen.queryByRole('heading', { name: 'Quote request received' })).toBeNull()
})
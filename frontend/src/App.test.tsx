// @vitest-environment jsdom

import { cleanup, render, screen, within } from '@testing-library/react'
import { afterEach, expect, it, vi } from 'vitest'
import App from './App'

const categories = [
    { id: 1, name: 'Marketing Materials' },
    { id: 2, name: 'Stationery & Forms' },
]

const products = [
    { id: 12, name: 'Business Cards', categoryId: 2 },
    { id: 5, name: 'Flyers', categoryId: 1 },
]

function mockCatalogApi(productResponse: () => Response) {
    vi.stubGlobal(
        'fetch',
        vi.fn(async (path: string) => {
            if (path === '/api/categories') {
                return Response.json(categories)
            }

            if (path === '/api/products') {
                return productResponse()
            }

            throw new Error(`Unexpected request: ${path}`)
        }),
    )
}

function productNamesUnder(heading: HTMLElement) {
    const section = heading.closest('section')

    if (!section) {
        throw new Error('Expected the category heading to be inside a section')
    }

    return within(section)
        .getAllByRole('listitem')
        .map((item) => item.textContent)
}

afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
})

it('shows each product under its own category', async () => {
    mockCatalogApi(() => Response.json(products))

    render(<App />)

    const marketing = await screen.findByRole('heading', {
        name: 'Marketing Materials (1)',
    })
    const stationery = screen.getByRole('heading', {
        name: 'Stationery & Forms (1)',
    })

    expect(productNamesUnder(marketing)).toEqual(['Flyers'])
    expect(productNamesUnder(stationery)).toEqual(['Business Cards'])
})

it('shows an error when the product API is unavailable', async () => {
    mockCatalogApi(() => new Response(null, { status: 503 }))

    render(<App />)

    const alert = await screen.findByRole('alert')

    expect(alert.textContent).toContain('Unable to load the product catalog.')
})
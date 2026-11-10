// @vitest-environment jsdom

import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router'
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

function mockCatalogApi(productResponse = () => Response.json(products)) {
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

function renderApp(path = '/') {
    render(
        <MemoryRouter initialEntries={[path]}>
            <App />
        </MemoryRouter>,
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
    mockCatalogApi()
    renderApp()

    const marketing = await screen.findByRole('heading', {
        name: 'Marketing Materials (1)',
    })
    const stationery = screen.getByRole('heading', {
        name: 'Stationery & Forms (1)',
    })

    expect(productNamesUnder(marketing)).toEqual(['Flyers'])
    expect(productNamesUnder(stationery)).toEqual(['Business Cards'])
})

it('opens a product from the catalog and returns to the catalog', async () => {
    mockCatalogApi()
    renderApp()

    const productLink = await screen.findByRole('link', { name: 'Business Cards' })
    expect(productLink.getAttribute('href')).toBe('/products/12')

    fireEvent.click(productLink)
    await screen.findByRole('heading', { level: 1, name: 'Business Cards' })

    fireEvent.click(screen.getByRole('link', { name: 'Back to catalog' }))
    await screen.findByRole('heading', { level: 1, name: 'Product Catalog' })
})

it('loads a product from its direct URL', async () => {
    mockCatalogApi()
    renderApp('/products/12')

    const heading = await screen.findByRole('heading', { level: 1 })

    expect(heading.textContent).toBe('Business Cards')
    expect(screen.getByRole('main').textContent).toContain(
        'Category: Stationery & Forms',
    )
})

it('shows a not-found page for an unknown product ID', async () => {
    mockCatalogApi()
    renderApp('/products/9999')

    const heading = await screen.findByRole('heading', { level: 1 })

    expect(heading.textContent).toBe('Product not found')
    expect(screen.getByRole('link', { name: 'Back to catalog' }).getAttribute('href'))
        .toBe('/')
})

it('shows an error when the product API is unavailable', async () => {
    mockCatalogApi(() => new Response(null, { status: 503 }))
    renderApp()

    const alert = await screen.findByRole('alert')

    expect(alert.textContent).toContain('Unable to load the product catalog.')
})
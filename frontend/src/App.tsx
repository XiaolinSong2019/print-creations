import { useEffect, useState } from 'react'
import { Link, Route, Routes, useParams } from 'react-router'
import QuoteRequestPage from './QuoteRequestPage'

type Category = { id: number; name: string }
type Product = { id: number; name: string; categoryId: number }
type Catalog = { categories: Category[]; products: Product[] }

async function fetchJson<T>(path: string, signal: AbortSignal): Promise<T> {
    const response = await fetch(path, { signal })

    if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
    }

    return (await response.json()) as T
}

function CatalogPage({ catalog }: { catalog: Catalog }) {
    return (
        <main>
            <h1>Product Catalog</h1>

            {catalog.categories.map((category) => {
                const categoryProducts = catalog.products.filter(
                    (product) => product.categoryId === category.id,
                )

                return (
                    <section key={category.id}>
                        <h2>{category.name} ({categoryProducts.length})</h2>
                        <ul>
                            {categoryProducts.map((product) => (
                                <li key={product.id}>
                                    <Link to={`/products/${product.id}`}>{product.name}</Link>
                                </li>
                            ))}
                        </ul>
                    </section>
                )
            })}
        </main>
    )
}

function NotFoundPage({ title }: { title: string }) {
    return (
        <main>
            <h1>{title}</h1>
            <p>Return to the catalog to browse available products.</p>
            <Link to="/">Back to catalog</Link>
        </main>
    )
}

function useCatalogProduct(products: Product[]) {
    const { productId } = useParams<'productId'>()
    return products.find((product) => String(product.id) === productId)
}

function ProductDetailsPage({ catalog }: { catalog: Catalog }) {
    const product = useCatalogProduct(catalog.products)

    if (!product) {
        return <NotFoundPage title="Product not found" />
    }

    const category = catalog.categories.find(
        (candidate) => candidate.id === product.categoryId,
    )

    return (
        <main>
            <Link to="/">Back to catalog</Link>
            <h1>{product.name}</h1>
            <p>Category: {category?.name ?? 'Unavailable'}</p>
            <Link to={`/products/${product.id}/quote`}>Request a Quote</Link>
        </main>
    )
}

function ProductQuoteRoute({ catalog }: { catalog: Catalog }) {
    const product = useCatalogProduct(catalog.products)

    if (!product) {
        return <NotFoundPage title="Product not found" />
    }

    return <QuoteRequestPage key={product.id} product={product} />
}

export default function App() {
    const [catalog, setCatalog] = useState<Catalog | null>(null)
    const [hasError, setHasError] = useState(false)

    useEffect(() => {
        const controller = new AbortController()

        Promise.all([
            fetchJson<Category[]>('/api/categories', controller.signal),
            fetchJson<Product[]>('/api/products', controller.signal),
        ])
            .then(([categories, products]) => {
                if (!controller.signal.aborted) {
                    setCatalog({ categories, products })
                }
            })
            .catch(() => {
                if (!controller.signal.aborted) {
                    setHasError(true)
                }
            })

        return () => controller.abort()
    }, [])

    if (hasError) {
        return <p role="alert">Unable to load the product catalog. Please try again.</p>
    }

    if (!catalog) {
        return <p role="status">Loading products...</p>
    }

    return (
        <Routes>
            <Route path="/" element={<CatalogPage catalog={catalog} />} />
            <Route
                path="/products/:productId"
                element={<ProductDetailsPage catalog={catalog} />}
            />
            <Route
                path="/products/:productId/quote"
                element={<ProductQuoteRoute catalog={catalog} />}
            />
            <Route path="*" element={<NotFoundPage title="Page not found" />} />
        </Routes>
    )
}
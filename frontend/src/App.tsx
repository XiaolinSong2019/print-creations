import { useEffect, useState } from 'react'

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
                      <li key={product.id}>{product.name}</li>
                  ))}
                </ul>
              </section>
          )
        })}
      </main>
  )
}
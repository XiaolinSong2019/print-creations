import { useState } from 'react'
import { Link } from 'react-router'

type Product = { id: number; name: string }
type QuoteProblem = { detail?: string; errors?: Record<string, string> }
type Submission =
    | { kind: 'idle' }
    | { kind: 'submitting' }
    | { kind: 'error'; message: string; errors: Record<string, string> }
    | { kind: 'success'; referenceNumber: string }

const fieldLabels: Record<string, string> = {
    firstName: 'First name',
    lastName: 'Last name',
    companyName: 'Company name',
    phone: 'Phone',
    email: 'Email',
    productId: 'Product',
    quantity: 'Quantity',
    fulfillmentMethod: 'Fulfillment method',
    neededBy: 'Needed by',
    specialInstructions: 'Special instructions',
    rushRequestedCompletionDate: 'Requested rush completion date',
    rushNotes: 'Rush notes',
}

function readText(data: FormData, name: string) {
    const value = data.get(name)
    return typeof value === 'string' ? value.trim() : ''
}

function buildQuotePayload(productId: number, data: FormData) {
    const rushRequested = data.has('rushRequested')

    return {
        productId,
        firstName: readText(data, 'firstName'),
        lastName: readText(data, 'lastName'),
        companyName: readText(data, 'companyName') || undefined,
        phone: readText(data, 'phone'),
        email: readText(data, 'email'),
        quantity: Number(readText(data, 'quantity')),
        fulfillmentMethod: readText(data, 'fulfillmentMethod'),
        neededBy: readText(data, 'neededBy') || undefined,
        specialInstructions: readText(data, 'specialInstructions') || undefined,
        rushRequested,
        rushRequestedCompletionDate: rushRequested
            ? readText(data, 'rushRequestedCompletionDate') || undefined
            : undefined,
        rushNotes: rushRequested ? readText(data, 'rushNotes') || undefined : undefined,
        marketingOptIn: data.has('marketingOptIn'),
    }
}

export default function QuoteRequestPage({ product }: { product: Product }) {
    const [rushRequested, setRushRequested] = useState(false)
    const [submission, setSubmission] = useState<Submission>({ kind: 'idle' })
    const isSubmitting = submission.kind === 'submitting'
    const productUrl = `/products/${product.id}`

    async function submitQuote(data: FormData) {
        if (isSubmitting) return

        setSubmission({ kind: 'submitting' })

        try {
            const response = await fetch('/api/quote-requests', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(buildQuotePayload(product.id, data)),
            })

            if (response.status === 400) {
                const problem = (await response.json()) as QuoteProblem | null
                setSubmission({
                    kind: 'error',
                    message: problem?.detail || 'Check your request and try again.',
                    errors: problem?.errors ?? {},
                })
                return
            }

            if (response.status !== 201) {
                throw new Error('Unexpected quote response')
            }

            const receipt = (await response.json()) as { referenceNumber?: string } | null

            if (typeof receipt?.referenceNumber !== 'string' || !receipt.referenceNumber.trim()) {
                throw new Error('Missing quote reference number')
            }

            setSubmission({ kind: 'success', referenceNumber: receipt.referenceNumber })
        } catch {
            setSubmission({
                kind: 'error',
                message: 'We could not confirm your quote request. Please contact us before submitting again.',
                errors: {},
            })
        }
    }

    if (submission.kind === 'success') {
        return (
            <main>
                <h1>Quote request received</h1>
                <p role="status">Reference number: <strong>{submission.referenceNumber}</strong></p>
                <Link to={productUrl}>Back to {product.name}</Link>
            </main>
        )
    }

    return (
        <main>
            <Link to={productUrl}>Back to {product.name}</Link>
            <h1>Request a Quote</h1>
            <p>Product: <strong>{product.name}</strong></p>

            {submission.kind === 'error' && (
                <div role="alert">
                    <p>{submission.message}</p>
                    {Object.keys(submission.errors).length > 0 && (
                        <ul>
                            {Object.entries(submission.errors).map(([field, message]) => (
                                <li key={field}>{fieldLabels[field] ?? 'Request'}: {message}</li>
                            ))}
                        </ul>
                    )}
                </div>
            )}

            <form
                aria-busy={isSubmitting}
                onSubmit={(event) => {
                    event.preventDefault()
                    void submitQuote(new FormData(event.currentTarget))
                }}
            >
                <fieldset disabled={isSubmitting}>
                    <legend>Contact information</legend>
                    <p><label>First name <input name="firstName" autoComplete="given-name" maxLength={100} required /></label></p>
                    <p><label>Last name <input name="lastName" autoComplete="family-name" maxLength={100} required /></label></p>
                    <p><label>Company name (optional) <input name="companyName" autoComplete="organization" maxLength={150} /></label></p>
                    <p><label>Phone <input name="phone" type="tel" autoComplete="tel" maxLength={40} required /></label></p>
                    <p><label>Email <input name="email" type="email" autoComplete="email" maxLength={254} required /></label></p>
                </fieldset>

                <fieldset disabled={isSubmitting}>
                    <legend>Project details</legend>
                    <p><label>Quantity <input name="quantity" type="number" min={1} max={2147483647} step={1} required /></label></p>
                    <p>
                        <label>
                            Fulfillment method{' '}
                            <select name="fulfillmentMethod" defaultValue="" required>
                                <option value="" disabled>Select a method</option>
                                <option value="PICKUP">Pickup</option>
                                <option value="MAILING">Mailing</option>
                                <option value="DELIVERY">Delivery</option>
                            </select>
                        </label>
                    </p>
                    <p><label>Needed by (optional) <input name="neededBy" type="date" /></label></p>
                    <p><label>Special instructions (optional) <textarea name="specialInstructions" maxLength={2000} /></label></p>
                </fieldset>

                <fieldset disabled={isSubmitting}>
                    <legend>Rush service</legend>
                    <label>
                        <input
                            name="rushRequested"
                            type="checkbox"
                            checked={rushRequested}
                            onChange={(event) => setRushRequested(event.target.checked)}
                        />
                        Request rush service
                    </label>
                    {rushRequested && (
                        <>
                            <p>Rush requests are subject to approval.</p>
                            <p><label>Requested rush completion date (optional) <input name="rushRequestedCompletionDate" type="date" /></label></p>
                            <p><label>Rush notes (optional) <textarea name="rushNotes" maxLength={1000} /></label></p>
                        </>
                    )}
                </fieldset>

                <p>
                    <label>
                        <input name="marketingOptIn" type="checkbox" disabled={isSubmitting} />
                        Send me promotional emails from Print Creations.
                    </label>
                </p>

                <button type="submit" disabled={isSubmitting}>
                    {isSubmitting ? 'Submitting...' : 'Submit quote request'}
                </button>
            </form>
        </main>
    )
}
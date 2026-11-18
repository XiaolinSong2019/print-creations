import { useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router'
import {
    AdminApiError,
    findAdminQuote,
    isQuoteReference,
    localAdminAuthorization,
    parseRushFee,
    saveRushDecision,
} from './adminRushApprovalApi'
import type { AdminQuote, RushDecision } from './adminRushApprovalApi'
import './adminRushApproval.css'

const usd = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' })

function errorMessage(error: unknown, saving: boolean): string {
    if (error instanceof AdminApiError) {
        switch (error.status) {
            case 400: return 'The server rejected the approval details. Check the fee and reload the quote.'
            case 401: return 'Admin authentication failed. Enter the local Admin password again.'
            case 403: return 'The server denied this Admin action. Check that the local Admin server is running.'
            case 404: return 'No quote was found for that reference.'
            case 409: return 'This quote changed or the fee conflicts. Reload it before making another decision.'
        }
    }
    return saving
        ? 'The decision could not be confirmed. Reload the quote to check its current state.'
        : 'Unable to load the quote. Check that the local server is running and try again.'
}

export default function AdminRushApprovalPage() {
    const [reference, setReference] = useState('')
    const [password, setPassword] = useState('')
    const [authorization, setAuthorization] = useState<string | null>(null)
    const [quote, setQuote] = useState<AdminQuote | null>(null)
    const [fee, setFee] = useState('')
    const [review, setReview] = useState<RushDecision | null>(null)
    const [needsReload, setNeedsReload] = useState(false)
    const [busy, setBusy] = useState<'lookup' | 'save' | null>(null)
    const [error, setError] = useState<string | null>(null)
    const [notice, setNotice] = useState<string | null>(null)
    const requestInProgress = useRef(false)

    function startRequest(action: 'lookup' | 'save') {
        if (requestInProgress.current) return false
        requestInProgress.current = true
        setBusy(action)
        setError(null)
        setNotice(null)
        return true
    }

    function endRequest() {
        requestInProgress.current = false
        setBusy(null)
    }

    function clearQuote() {
        setQuote(null)
        setReview(null)
        setFee('')
        setNeedsReload(false)
    }

    function handleError(cause: unknown, saving: boolean) {
        if (cause instanceof AdminApiError && cause.status === 401) {
            setAuthorization(null)
            setPassword('')
            clearQuote()
        } else if (saving) {
            setNeedsReload(true)
        }
        setError(errorMessage(cause, saving))
    }

    async function lookup(id: string, auth: string) {
        if (!startRequest('lookup')) return
        clearQuote()
        try {
            setQuote(await findAdminQuote(id, auth))
            setAuthorization(auth)
        } catch (cause) {
            handleError(cause, false)
        } finally {
            endRequest()
        }
    }

    function submitLookup(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        const id = reference.trim()
        if (!isQuoteReference(id)) {
            setError('Enter the complete quote reference.')
            return
        }
        if (authorization === null && password.length === 0) {
            setError('Enter the local Admin password.')
            return
        }
        const auth = authorization ?? localAdminAuthorization(password)
        setPassword('')
        setReference(id)
        void lookup(id, auth)
    }

    function reviewApproval(event: FormEvent<HTMLFormElement>) {
        event.preventDefault()
        const amount = parseRushFee(fee)
        if (amount === null) {
            setError('Enter a nonnegative USD amount with up to two decimal places and ten whole-number digits.')
            return
        }
        setError(null)
        setNotice(null)
        setReview({ rushApproved: true, rushFeeAmount: amount })
    }

    async function confirmDecision() {
        if (!quote || !authorization || !review || needsReload || !startRequest('save')) return
        const decision = review
        setReview(null)
        try {
            const saved = await saveRushDecision(quote, decision, authorization)
            setQuote({ ...quote, ...saved })
            setFee('')
            setNotice(saved.rushApproved ? 'Rush approval saved.' : 'Rush approval revoked.')
        } catch (cause) {
            handleError(cause, true)
        } finally {
            endRequest()
        }
    }

    return (
        <main className="admin-rush-page">
            <div className="admin-rush-toolbar">
                <Link to="/">Back to catalog</Link>
                {authorization && (
                    <button type="button" className="admin-rush-secondary" disabled={busy !== null}
                        onClick={() => {
                            setAuthorization(null)
                            setReference('')
                            setPassword('')
                            setError(null)
                            setNotice(null)
                            clearQuote()
                        }}>
                        Clear Admin session
                    </button>
                )}
            </div>
            <h1>Rush approvals</h1>
            <p>Find a quote by its reference to review the customer’s rush request.</p>
            <form className="admin-rush-card admin-rush-form" onSubmit={submitLookup} aria-busy={busy === 'lookup'}>
                <label htmlFor="admin-quote-reference">Quote reference</label>
                <input id="admin-quote-reference" value={reference} autoComplete="off" required
                    spellCheck={false} disabled={busy !== null} onChange={(event) => {
                        setReference(event.target.value)
                        setError(null)
                        setNotice(null)
                        clearQuote()
                    }} />
                {!authorization && (
                    <>
                        <label htmlFor="admin-local-password">Local Admin password</label>
                        <input id="admin-local-password" type="password" autoComplete="current-password"
                            value={password} required disabled={busy !== null}
                            onChange={(event) => setPassword(event.target.value)} />
                    </>
                )}
                <button type="submit" disabled={busy !== null}>
                    {busy === 'lookup' ? 'Finding quote…' : 'Find quote'}
                </button>
            </form>

            {error && <p className="admin-rush-error" role="alert">{error}</p>}
            {notice && <p className="admin-rush-notice" role="status">{notice}</p>}

            {quote && (
                <section className="admin-rush-card" aria-labelledby="admin-rush-quote-heading">
                    <h2 id="admin-rush-quote-heading">Quote details</h2>
                    <dl className="admin-rush-details">
                        <div><dt>Reference</dt><dd>{quote.referenceNumber}</dd></div>
                        <div><dt>Customer</dt><dd>{quote.firstName} {quote.lastName}</dd></div>
                        <div><dt>Quote status</dt><dd>{quote.status}</dd></div>
                        <div><dt>Quantity</dt><dd>{quote.quantity}</dd></div>
                        <div><dt>Fulfillment</dt><dd>{quote.fulfillmentMethod}</dd></div>
                        <div><dt>Rush decision</dt><dd>
                            {quote.rushApproved ? 'Approved' : quote.rushRequested ? 'Pending approval' : 'Not requested'}
                        </dd></div>
                        {quote.rushFeeAmount !== null && (
                            <div><dt>Rush fee</dt><dd>{usd.format(quote.rushFeeAmount)}</dd></div>
                        )}
                        {quote.rushRequestedCompletionDate && (
                            <div><dt>Requested completion</dt><dd>{quote.rushRequestedCompletionDate}</dd></div>
                        )}
                        {quote.rushNotes && <div><dt>Rush notes</dt><dd>{quote.rushNotes}</dd></div>}
                    </dl>

                    {needsReload && (
                        <button type="button" disabled={busy !== null}
                            onClick={() => { if (authorization) void lookup(quote.referenceNumber, authorization) }}>
                            Reload quote
                        </button>
                    )}
                    {!needsReload && quote.rushRequested && !quote.rushApproved && !review && (
                        <form className="admin-rush-form" onSubmit={reviewApproval}>
                            <label htmlFor="admin-rush-fee">Rush fee (USD)</label>
                            <input id="admin-rush-fee" inputMode="decimal" placeholder="0.00" required
                                value={fee} onChange={(event) => setFee(event.target.value)} disabled={busy !== null} />
                            <button type="submit" disabled={busy !== null}>Review approval</button>
                        </form>
                    )}
                    {!needsReload && quote.rushApproved && !review && (
                        <button type="button" disabled={busy !== null} onClick={() => {
                            setError(null)
                            setNotice(null)
                            setReview({ rushApproved: false, rushFeeAmount: null })
                        }}>
                            Review revocation
                        </button>
                    )}
                    {review && (
                        <div className="admin-rush-review" role="group" aria-label="Review rush decision">
                            <p>{review.rushApproved
                                ? `Approve rush service with a fee of ${usd.format(review.rushFeeAmount)}?`
                                : 'Revoke rush approval and clear the fee?'}</p>
                            <button type="button" onClick={() => { void confirmDecision() }} disabled={busy !== null}>
                                {review.rushApproved ? 'Confirm approval' : 'Confirm revocation'}
                            </button>
                            <button type="button" className="admin-rush-secondary"
                                onClick={() => setReview(null)} disabled={busy !== null}>Cancel</button>
                        </div>
                    )}
                    {busy === 'save' && <p role="status">Saving rush decision…</p>}
                </section>
            )}
        </main>
    )
}

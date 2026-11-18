export type RushState = {
    referenceNumber: string
    rushRequested: boolean
    rushApproved: boolean
    rushFeeAmount: number | null
    version: number
}

export type AdminQuote = RushState & {
    status: string
    firstName: string
    lastName: string
    quantity: number
    fulfillmentMethod: string
    rushRequestedCompletionDate: string | null
    rushNotes: string | null
}

export type RushDecision =
    | { rushApproved: true; rushFeeAmount: number }
    | { rushApproved: false; rushFeeAmount: null }

export class AdminApiError extends Error {
    readonly status: number

    constructor(status: number) {
        super(`Admin request failed with HTTP ${status}.`)
        this.status = status
    }
}

export function localAdminAuthorization(password: string): string {
    const bytes = new TextEncoder().encode(`admin:${password}`)
    return `Basic ${btoa(Array.from(bytes, (byte) => String.fromCharCode(byte)).join(''))}`
}

export function isQuoteReference(reference: string): boolean {
    return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(reference)
}

export function parseRushFee(input: string): number | null {
    const amount = input.trim()
    return /^\d{1,10}(?:\.\d{1,2})?$/.test(amount) ? Number(amount) : null
}

function responseObject(value: unknown): Record<string, unknown> {
    if (typeof value !== 'object' || value === null || Array.isArray(value)) {
        throw new Error('The server did not return the expected Admin data.')
    }
    return value as Record<string, unknown>
}

function readRushState(data: Record<string, unknown>, reference: string): RushState {
    const { referenceNumber, rushRequested, rushApproved, rushFeeAmount, version } = data
    if (
        typeof referenceNumber !== 'string' || referenceNumber.toLowerCase() !== reference.toLowerCase()
        || typeof rushRequested !== 'boolean' || typeof rushApproved !== 'boolean'
        || typeof version !== 'number' || !Number.isSafeInteger(version) || version < 0
        || (rushApproved && (!rushRequested || typeof rushFeeAmount !== 'number'
            || !Number.isFinite(rushFeeAmount) || rushFeeAmount < 0))
        || (!rushApproved && rushFeeAmount !== null && rushFeeAmount !== undefined)
    ) {
        throw new Error('The server returned an invalid rush decision.')
    }
    return {
        referenceNumber, rushRequested, rushApproved, version,
        rushFeeAmount: typeof rushFeeAmount === 'number' ? rushFeeAmount : null,
    }
}

async function adminRequest(
    path: string,
    authorization: string,
    options: RequestInit = {},
): Promise<Record<string, unknown>> {
    const headers = new Headers(options.headers)
    headers.set('Authorization', authorization)
    headers.set('Accept', 'application/json')
    headers.set('X-Requested-With', 'XMLHttpRequest')

    const response = await fetch(path, {
        ...options,
        headers,
        credentials: 'same-origin',
        cache: 'no-store',
        redirect: 'error',
    })
    if (!response.ok) {
        throw new AdminApiError(response.status)
    }
    return responseObject(await response.json())
}

export async function findAdminQuote(reference: string, authorization: string): Promise<AdminQuote> {
    const data = await adminRequest(`/api/admin/quotes/${encodeURIComponent(reference)}`, authorization)
    const { status, firstName, lastName, quantity, fulfillmentMethod } = data
    if (
        typeof status !== 'string' || typeof firstName !== 'string' || typeof lastName !== 'string'
        || typeof quantity !== 'number' || !Number.isSafeInteger(quantity) || quantity < 1
        || typeof fulfillmentMethod !== 'string'
    ) {
        throw new Error('The server returned invalid quote details.')
    }
    return {
        ...readRushState(data, reference),
        status, firstName, lastName, quantity, fulfillmentMethod,
        rushRequestedCompletionDate: typeof data.rushRequestedCompletionDate === 'string'
            ? data.rushRequestedCompletionDate : null,
        rushNotes: typeof data.rushNotes === 'string' ? data.rushNotes : null,
    }
}

export async function saveRushDecision(
    quote: RushState,
    decision: RushDecision,
    authorization: string,
): Promise<RushState> {
    const csrf = await adminRequest('/api/admin/csrf', authorization)
    if (
        typeof csrf.headerName !== 'string' || !/^x-[a-z0-9-]+$/i.test(csrf.headerName)
        || typeof csrf.token !== 'string' || csrf.token.length === 0
    ) {
        throw new Error('The server did not return an approval token.')
    }

    const result = await adminRequest(
        `/api/admin/quotes/${encodeURIComponent(quote.referenceNumber)}/rush-approval`,
        authorization,
        {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json', [csrf.headerName]: csrf.token },
            body: JSON.stringify({ ...decision, expectedVersion: quote.version }),
        },
    )
    return readRushState(result, quote.referenceNumber)
}

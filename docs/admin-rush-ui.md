# PC-010: Admin Rush Approval Page

The Admin route is `/admin/quotes`. It looks up a quote by reference,
shows the customer’s rush request, and supports approving or revoking the
decision after review. The browser sends the version it read. A conflict
blocks a second decision until the Admin reloads the quote.

The page uses the existing same-origin `/api` proxy during Vite development.
Credentials for the `local-admin` Spring profile are held in page memory.
The page requests a CSRF token before each write and lets the browser send
the associated session cookie. Clear Admin session removes the credentials
and quote from the page.

## Local verification

Start PostgreSQL and the Spring Boot app with the existing `local-admin`
profile. In a second terminal, from the repository root:

```bash
cd frontend
npm ci
npm run lint
npm test
npm run build
npm run dev
```

1. Submit a new rush quote in the customer UI and copy its reference.
2. Open `/admin/quotes` on the Vite origin. Enter the reference and the local
   Admin password. Confirm the quote status is NEW and rush is pending.
3. Enter 25.50 and review the approval. Cancel once and verify no approval
   request was sent. Review again and confirm.
4. Refresh the page, look up the same quote, and verify Approved and $25.50.
   Query PostgreSQL using the reference to confirm a single audit row.
5. Review and confirm revocation. Reload and confirm the fee is cleared and
   a second audit row records revocation. The quote status remains NEW.

To inspect the saved decision and its audit, run from the repository root.
Replace the sample UUID with the fresh test quote’s reference:

```bash
pc010_reference='51afdb79-7779-40ac-85ac-2f1722542a79'
docker compose exec -T postgres psql -U print_creations -d print_creations \
  -v ON_ERROR_STOP=1 -v "quote_reference=$pc010_reference" <<'SQL'
SELECT status, rush_requested, rush_approved, rush_fee_amount, version
FROM quote_requests
WHERE reference_number = :'quote_reference'::uuid;

SELECT actor, old_version, new_version, old_rush_approved, new_rush_approved,
       old_rush_fee_amount, new_rush_fee_amount
FROM quote_rush_approval_audit
WHERE reference_number = :'quote_reference'::uuid
ORDER BY new_version;
SQL
```

Record the browser and database results in the development log after running
the checks.

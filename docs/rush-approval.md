# PC-009: Rush Approval

## Scope

An authenticated Admin can approve or clear the rush decision on an existing
quote. A fee is entered in USD by the Admin. This operation preserves the
customer's request, requested date, notes, quote status, and marketing consent.
It has no email or order status history side effects.

The earlier PC-101 design used a numeric order ID. This repository currently
persists quotes, so the route uses the quote reference UUID.

## API contract

1. Read `GET /api/admin/quotes/{referenceNumber}` to obtain `version`.
2. Read `GET /api/admin/csrf` and retain the session cookie. Use the returned
   `headerName` and `token` on the update request.
3. Submit `PATCH /api/admin/quotes/{referenceNumber}/rush-approval` with the
   same session cookie, Admin credentials, and an `application/json` body.

```json
{
  "rushApproved": true,
  "rushFeeAmount": 25.50,
  "expectedVersion": 0
}
```

Amounts follow PC-101: nonnegative USD, at most ten integer digits and two
fractional digits. The API rejects extra precision rather than rounding it.
To clear an approval, send `rushApproved: false`, a null fee, and the current
version. `expectedVersion` is a precondition; the client cannot set the
stored version directly.

| Status | Meaning |
| --- | --- |
| 200 | Decision saved, or requested approval and fee already match the saved state. |
| 400 | Invalid body, fee, version, or unexpected field. |
| 401 | Valid CSRF token but missing or invalid authentication. |
| 403 | Missing or invalid CSRF token, or an authenticated user without the Admin role. |
| 404 | Quote reference does not exist. |
| 409 | Rush was not requested, an approved fee differs, or a state-changing request uses a stale version. |

A successful response includes `referenceNumber`, `rushRequested`,
`rushApproved`, `rushFeeAmount`, and `version`.

## Data and transaction decision

`quote_requests.reference_number` (PK) → `quote_rush_approval_audit.reference_number` (FK).
The audit table has one row per `(reference_number, new_version)`, with the
authenticated actor, timestamp, and old and new versions, decisions, and fees.
Migration V2 adds the version with a default of zero.

Use the PC-101 optimistic compare-and-set strategy. Under PostgreSQL Read
Committed isolation, `UPDATE ... WHERE version = :expectedVersion` prevents
a stale writer from replacing a winning update. If another writer wins,
read the committed state and return it when the decision already matches;
otherwise return 409. A stale retry that already matches is a no-op. A future
version is rejected. Updating the quote and inserting the audit event share
one transaction, so an audit failure rolls back the quote update.

## Automated verification

Start Docker Desktop and run from the repository root:

```bash
./mvnw clean org.jacoco:jacoco-maven-plugin:0.8.14:prepare-agent \
  verify org.jacoco:jacoco-maven-plugin:0.8.14:report
```

The integration tests use PostgreSQL through Testcontainers. They cover
approval, clearing, identical retries, fee and version conflicts, invalid
input, authorization and CSRF, concurrency, and audit rollback. Record the
actual test summary and the `RushApprovalService` branch coverage from
`target/site/jacoco/jacoco.csv` in the development log.

## Local smoke check

Start the development database with Docker Compose. Start Spring Boot with
the existing `local-admin` profile and supply `PRINT_CREATIONS_ADMIN_PASSWORD`
at runtime. Use an existing test quote with `rushRequested: true`,
`rushApproved: false`, and `version: 0`, or submit a fresh test quote.

In another terminal, replace the sample reference with the test quote's UUID:

```bash
pc009_reference='8e918dd8-f576-48cc-b6f0-8c563fa678d4'
pc009_api='http://127.0.0.1:8080/api/admin'
curl -fsS --user admin "$pc009_api/quotes/$pc009_reference"
```

Confirm the quote version before sending the update. The following example
uses version zero. Each curl command that uses `--user admin` prompts for
the local Admin password.

```bash
pc009_cookie_jar="$(mktemp "${TMPDIR:-/tmp}/pc009-cookies.XXXXXX")"
pc009_csrf="$(curl -fsS --user admin --cookie-jar "$pc009_cookie_jar" "$pc009_api/csrf")"
pc009_header="$(printf '%s' "$pc009_csrf" | node -p 'JSON.parse(require("node:fs").readFileSync(0,"utf8")).headerName')"
pc009_token="$(printf '%s' "$pc009_csrf" | node -p 'JSON.parse(require("node:fs").readFileSync(0,"utf8")).token')"

curl -sS -i --request PATCH --user admin \
  --cookie "$pc009_cookie_jar" \
  --header "$pc009_header: $pc009_token" \
  --header 'Content-Type: application/json' \
  --data '{"rushApproved":true,"rushFeeAmount":25.50,"expectedVersion":0}' \
  "$pc009_api/quotes/$pc009_reference/rush-approval"

rm -f -- "$pc009_cookie_jar"
unset pc009_csrf pc009_token
```

Expect HTTP 200, `rushApproved: true`, `rushFeeAmount: 25.50`, and `version: 1`.
Read the same quote again and confirm that `status` is still `NEW`. Verify
the audit row in the development database:

```bash
docker compose exec -T postgres psql -U print_creations -d print_creations \
  -v "quote_reference=$pc009_reference" <<'SQL'
SELECT actor, old_version, new_version, old_rush_approved, new_rush_approved,
       old_rush_fee_amount, new_rush_fee_amount, changed_at
FROM quote_rush_approval_audit
WHERE reference_number = :'quote_reference'::uuid
ORDER BY new_version;
SQL
```

# Development Log

## PC-000: Project Initialization

Date: 2026-10-22
Status: Done

### Work Completed

- Confirmed that the project was already a Git repository.
- Confirmed that the main branch had no commits at initialization.
- Reviewed the Maven configuration, application entry point,
  and application context test.
- Confirmed that the reviewed source changes only affected indentation.
- Reviewed the application configuration.
- Created the initial README and development log.

### Previous Verification

The following results were recorded on October 21, 2026:

- Maven build: successful.
- Tests: 1 passed, 0 failures, 0 errors, 0 skipped.
- Packaged application: started successfully on port 8080.
- Root path: returned HTTP 404.

Business workflows were not tested during project initialization.

### Completion Record

Completed: 2026-10-23

- Initial commit: `9599413`.
- Commit message: `chore: initialize Spring Boot project and documentation`.
- Initial commit pushed to `origin/main`.
- Local and remote main branches were synchronized after the initial push.
- Working tree was clean after the initial push.
- GitHub authentication completed using GitHub CLI.

## PC-001: V1 Requirements Baseline

Status: Done
Pull Request: https://github.com/XiaolinSong2019/print-creations/pull/2
Main Branch Commit: `04d74aa`

### Work Completed

- Documented the initial V1 scope, user roles, and exclusions
  in `docs/requirements-v1.md`.
- Defined acceptance criteria for quote requests, artwork uploads,
  admin rush approval, and order status updates.
- Merged the initial requirements baseline through pull request #2.

### Follow-up Design

Detailed rules will be refined during feature design, including:

- Upload size and file count limits.
- Rush fee precision and rounding.
- Marketing preference updates for existing customers.
- Allowed order status transitions.
- Authorization rejection cases.
- Atomic persistence of changes and their audit or history records.

## PC-002: Public Category Listing API

Date: 2026-10-28
Status: Done
Branch: `feat/pc-002-category-api`

### Work Completed

- Added public GET /api/categories.
- Added CategoryResponse with numeric id and name fields.
- Defined an in-memory catalog containing four approved categories
  with stable IDs from 1 through 4.
- Added an MVC contract test covering HTTP status, JSON content type,
  array size, category order, IDs, and names.
- Reviewed Maven configuration and retained the Java 21 build baseline

### Verification

- Manual command: `curl -i http://localhost:8080/api/categories`.
- Manual result: HTTP 200, application/json, and four categories
  with the expected IDs and names.
- Automated command: `./mvnw test`.
- Automated results: 2 tests passed, 0 failures, 0 errors, 0 skipped.
- Maven result: BUILD SUCCESS.
- Clean build command: `./mvnw clean verify`.
- Clean build result: BUILD SUCCESS, 2 tests passed, executable JAR generated

### Completion Record

Completed: 2026-10-28
Pull Request: [PR #4](https://github.com/XiaolinSong2019/print-creations/pull/4)
Main Branch Commit: `3fa12e5`

## PC-003: Maven CI

Status: Done
Pull Request: [PR #6](https://github.com/XiaolinSong2019/print-creations/pull/6)
Main Branch Commit: `fe468ef`

- The PR build passed with 2 tests.
- The push to `main` triggered Maven CI run `42345796492`.

## PC-004: Public Product Catalog API

Date: 2026-11-09
Status: Done

- Added `GET /api/products` for the 28 approved products.
- Verified HTTP 200 and the product list with curl.
- Added an MVC test for the total, category counts, and the Business Cards category.
- `./mvnw test`: 3 passed, 0 failures, 0 errors, 0 skipped.
- Pull Request: [#8](https://github.com/XiaolinSong2019/print-creations/pull/8)
  Main Branch Commit: `041761c`

## PC-005: Frontend Product Catalog

Date: 2026-11-09
Status: Done
Issue: [#9](https://github.com/XiaolinSong2019/print-creations/issues/9)

- Displayed 28 products under four categories using the Spring Boot APIs.
- Verified category counts of 10, 11, 4, and 3 in the browser.
- Passed two component tests covering product grouping and the API error state.
- Passed local lint and production build checks.
- Configured Frontend CI to install from the npm lockfile and run lint, tests, and build.
- The production dependency audit reported no vulnerabilities. Follow up on
  [GHSA-g3hv-75wg-m249](https://github.com/advisories/GHSA-g3hv-75wg-m249)
  when a compatible update is available for the ESLint toolchain.

## PC-006: Public Product Detail Pages

Date: 2026-11-09
Status: In Progress
Issue: [#11](https://github.com/XiaolinSong2019/print-creations/issues/11)

- Linked catalog products to a shared detail page at `/products/:productId`.
- Verified catalog navigation, direct links, refresh, and the missing-product page in the browser.
- Passed five frontend tests, lint, and the production build.
- The production dependency audit reported zero vulnerabilities.
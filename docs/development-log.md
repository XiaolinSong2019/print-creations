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
Status: In Progress
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

### Next Steps
- Complete documentation and staged diff review.
- Commit the changes, push the feature branch, and create a pull request.
- Merge the feature into main after review.
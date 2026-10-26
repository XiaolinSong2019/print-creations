# V1 Requirements

Status: Draft

## Product Goal

Provide a print shop website where customers can explore products
and submit quote or order requests, with admin tools for managing
customers and orders.

## User Roles

- Customer: browses products and submits requests.
- Admin: manages customers, orders, promotions, and customer communications.

## V1 Scope

- Responsive public pages based on the approved prototype, including
  products, promotions, services, industry pages, About, Contact, FAQ,
  and the File Preparation Guide.
- A catalog containing the 28 approved products and their options.
- Quote and order requests without customer registration.
- Optional artwork uploads, file validation, and print-readiness warnings.
- Customer rush requests, separate admin approval, and manual rush fees.
- Authenticated admin tools for managing customers, orders, and catalog data.
- Order status updates with review, explicit confirmation, and history.
- Transactional emails for request confirmations and order status updates.
- Order email templates with order selection, editable drafts, and review.
- Website promotions and email campaigns with recipient filtering,
  unsubscribe handling, and explicit send confirmation.

## Out of Scope

- Online checkout, payment gateways, or collection of card information.
- Customer accounts or a customer login portal.
- In-app chat.
- Online proof approval.
- Inventory or ERP functionality.
- AI-based artwork evaluation or recommendations.

## User Stories and Acceptance Criteria

### US-001: Submit a Quote Request

As a customer,
I want to submit my project details without creating an account,
so that Print Creations can review my request and provide a quote.

#### Required Fields

- First Name
- Last Name
- Phone
- Email
- Product
- Quantity
- Fulfillment Method: Pickup, Mailing, or Delivery

#### Optional Information

- Company Name
- Needed By
- Product-specific options
- Special Instructions
- Artwork files
- Rush request, requested completion date, and rush notes
- Marketing opt-in

#### Acceptance Criteria

**AC-001.1: Submit a valid request**

Given a visitor who is not signed in,
when all required fields contain valid values and the request is submitted,
then the request is saved with a unique reference number and New status,
and the customer sees a confirmation containing that reference number.
An authorized admin can retrieve the saved request after a page refresh
or application restart.

**AC-001.2: Reject invalid input**

Given a request with a missing required field, an invalid email address,
or a quantity that is not a positive integer,
when submission is attempted,
then the customer receives a clear validation message,
and no request is created.

The backend must also reject invalid input independently of browser validation.

**AC-001.3: Allow optional artwork**

Given an otherwise valid request,
when the customer provides no artwork or supported partial artwork,
then submission is allowed.

Print-readiness warnings alone must not prevent submission.

**AC-001.4: Keep rush requests separate from approval**

Given a valid request with rush service selected,
when the request is saved,
then the rush request and supplied rush details are recorded,
rush approval remains false,
and no rush fee is automatically calculated or added.

Only an authorized admin can approve rush service and enter its fee.

**AC-001.5: Keep marketing opt-in optional**

Given a new customer,
when the quote form is opened,
then marketing opt-in is unchecked.

When the customer submits without selecting it,
then the request is accepted without enrolling the customer in marketing emails.

**AC-001.6: Send request notifications**

Given a successfully saved request,
when request notifications are processed,
then the customer receives a request confirmation,
and the company receives an internal request notification.

These transactional notifications do not depend on marketing opt-in.
Email failures are recorded for retry and do not discard the saved request.
Retrying notification delivery must not create another request
or resend notifications already recorded as successfully delivered.

## Open Questions

- Maximum file size and file count require confirmation against
  the deployment environment.
- Existing-customer marketing preference updates require separate
  acceptance scenarios.
### US-002: Upload Artwork

As a customer,
I want to attach finished or partial artwork to my request,
so that Print Creations can review the materials for my project.

Artwork uploads remain optional, as defined in AC-001.3.

Supported extensions, matched case-insensitively:

- PDF: .pdf
- JPEG: .jpg, .jpeg
- TIFF: .tif, .tiff
- EPS: .eps

#### Acceptance Criteria

**AC-002.1: Accept supported files**

Given supported files that pass validation,
when the customer submits a valid request,
then the files are stored and associated with that request.

The original filename, detected file type, and file size are recorded.

**AC-002.2: Reject invalid files**

Given an unsupported or unreadable file,
when validation is performed,
then a clear file-specific error is displayed,
and submission is blocked until the file is removed or replaced.

Backend validation checks file content in addition to the filename
and declared content type.

**AC-002.3: Allow print-readiness warnings**

Given supported artwork with resolution, color, or bleed concerns,
when available print-readiness checks identify a concern,
then the concern is shown as a warning requiring admin review,
and submission remains available.

Checks that cannot determine print readiness require admin review.

**AC-002.4: Enforce upload limits**

Given configured file size and file count limits,
when a selected upload exceeds a limit,
then a clear error identifies the exceeded limit,
and submission is blocked until the upload meets the limits.

The backend enforces these limits independently of browser checks.
Exact limits remain listed under Open Questions until confirmed.

**AC-002.5: Handle upload failures**

Given an upload that fails,
when the failure is detected,
then the customer sees an error and can retry or remove the file.

Valid text fields are preserved, and the failed file is not shown
as successfully uploaded.

**AC-002.6: Protect stored artwork**

Given artwork stored for a request,
when an authorized admin opens that request,
then the admin can retrieve its associated files.

Unauthenticated users cannot list or download stored customer artwork.

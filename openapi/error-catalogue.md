# Amen API error catalogue

Generated from `openapi/openapi.yml` (`error` arrays in the documented examples). The API returns errors as:

```json
{ "error": ["<code>", "..."] }
```

Codes follow the pattern `<field>__<problem>` for validation errors and `<object>__<problem>` for business rules.

| Code | HTTP | Meaning (from spec) | Recommended handling | Seen on |
|---|---|---|---|---|
| `action__not_allowed` | 403 | Deal with status different from draft cannot be updated | Business rule violation. Surface the message to the operator; do not retry automatically. | DELETE /api/v1/deals/{deal_number}<br>GET /api/v1/deals/{deal_number}/allowed-payment-methods/<br>POST /api/v1/deals/{deal_number}/action/approve<br>POST /api/v1/deals/{deal_number}/action/cancel<br>P |
| `amount__invalid` | 400 | Amount is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/withdrawals/ |
| `amount__max` | 400 | Amount is greater than maximum allowed | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/withdrawals/ |
| `amount__min` | 400 | Amount is less than minimum allowed | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/withdrawals/ |
| `amount__required` | 400 | Amount is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/withdrawals/ |
| `api_access__disabled` | 403 | API is not active | Business rule violation. Surface the message to the operator; do not retry automatically. | DELETE /api/v1/account/bank-accounts/{bank_account_id}<br>DELETE /api/v1/deals/{deal_number}<br>DELETE /api/v1/web-hooks/{webhook_id}<br>GET /api/v1/account<br>GET /api/v1/account/bank-accounts/<br>GE |
| `api_token__invalid` | 403 | API token is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | DELETE /api/v1/account/bank-accounts/{bank_account_id}<br>DELETE /api/v1/deals/{deal_number}<br>DELETE /api/v1/web-hooks/{webhook_id}<br>GET /api/v1/account<br>GET /api/v1/account/bank-accounts/<br>GE |
| `api_token__required` | 403 | API token is required | Client-side validation: the field is missing. Fix the request; do not retry. | DELETE /api/v1/account/bank-accounts/{bank_account_id}<br>DELETE /api/v1/deals/{deal_number}<br>DELETE /api/v1/web-hooks/{webhook_id}<br>GET /api/v1/account<br>GET /api/v1/account/bank-accounts/<br>GE |
| `attachment_1__invalid` | 400 | Attachment 1 file is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/dispute<br>POST /api/v1/deals/{deal_number}/action/dispute-approve<br>POST /api/v1/deals/{deal_number}/action/dispute-decline |
| `attachment_1__too_large` | 400 | Attachment 1 file is too large | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/deals/{deal_number}/action/dispute<br>POST /api/v1/deals/{deal_number}/action/dispute-approve<br>POST /api/v1/deals/{deal_number}/action/dispute-decline |
| `attachment_2__invalid` | 400 | Attachment 2 file is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/dispute<br>POST /api/v1/deals/{deal_number}/action/dispute-approve<br>POST /api/v1/deals/{deal_number}/action/dispute-decline |
| `attachment_2__too_large` | 400 | Attachment 2 file is too large | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/deals/{deal_number}/action/dispute<br>POST /api/v1/deals/{deal_number}/action/dispute-approve<br>POST /api/v1/deals/{deal_number}/action/dispute-decline |
| `attachment_3__invalid` | 400 | Attachment 3 file is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/dispute<br>POST /api/v1/deals/{deal_number}/action/dispute-approve<br>POST /api/v1/deals/{deal_number}/action/dispute-decline |
| `attachment_3__too_large` | 400 | Attachment 3 file is too large | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/deals/{deal_number}/action/dispute<br>POST /api/v1/deals/{deal_number}/action/dispute-approve<br>POST /api/v1/deals/{deal_number}/action/dispute-decline |
| `bank__unknown` | 400 | Unknown bank | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/account/bank-accounts/ |
| `bank_account__not_found` | 404 | Bank account not found | The referenced object does not exist in this account/environment. Do not retry. | DELETE /api/v1/account/bank-accounts/{bank_account_id} |
| `bank_account_id__invalid` | 400 | Bank account ID is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/withdrawals/ |
| `bank_account_id__required` | 400 | Bank account ID is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/withdrawals/ |
| `building_number__invalid` | 400 | Building number is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/billing-address<br>POST /api/v1/deals/{deal_number}/delivery-address |
| `building_number__required` | 400 | Building number is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/billing-address<br>POST /api/v1/deals/{deal_number}/delivery-address |
| `buyers__invalid` | 400 | Buyers should be a list | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/parties/ |
| `buyers__limit_exceeded` | 400 | Only one buyer is allowed for the Deal | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/deals/{deal_number}/parties/ |
| `buyers__required` | 400 | Buyers list is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/parties/ |
| `buyers_idx__already_added` | 400 | Buyer cannot be also a seller for the same Deal | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/deals/{deal_number}/parties/ |
| `buyers_idx__not_found` | 400 | Buyer (Customer) with specified number does not exist | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/deals/{deal_number}/parties/ |
| `call__not_allowed` | 403 | API call is not allowed | Business rule violation. Surface the message to the operator; do not retry automatically. | DELETE /api/v1/account/bank-accounts/{bank_account_id}<br>DELETE /api/v1/deals/{deal_number}<br>DELETE /api/v1/web-hooks/{webhook_id}<br>GET /api/v1/account<br>GET /api/v1/account/bank-accounts/<br>GE |
| `city__invalid` | 400 | City ID is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/billing-address<br>POST /api/v1/deals/{deal_number}/delivery-address |
| `city__required` | 400 | City ID is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/billing-address<br>POST /api/v1/deals/{deal_number}/delivery-address |
| `comment__invalid` | 400 | Dispute comment is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/cancel<br>POST /api/v1/deals/{deal_number}/action/dispute<br>POST /api/v1/deals/{deal_number}/action/dispute-approve<br>POST /api/v1/deals/{deal_number}/action/ |
| `comment__required` | 400 | Dispute comment is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/cancel<br>POST /api/v1/deals/{deal_number}/action/dispute<br>POST /api/v1/deals/{deal_number}/action/dispute-approve<br>POST /api/v1/deals/{deal_number}/action/ |
| `customer__limit_reached` | 429 | Max number of customers reached | Back off exponentially (honour Retry-After if present) and retry. | POST /api/v1/customers/ |
| `customer__not_found` | 404 | Customer with specified number does not exist | The referenced object does not exist in this account/environment. Do not retry. | GET /api/v1/customers/{customer_number}<br>POST /api/v1/deals/{deal_number}/parties/ |
| `date_from__invalid` | 400 | Date from is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | GET /api/v1/withdrawals/ |
| `date_to__invalid` | 400 | Date to is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | GET /api/v1/withdrawals/ |
| `deal__limit_reached` | 429 | Max number of deals reached | Back off exponentially (honour Retry-After if present) and retry. | POST /api/v1/deals/ |
| `deal__not_found` | 404 | Deal with specified number does not exist | The referenced object does not exist in this account/environment. Do not retry. | DELETE /api/v1/deals/{deal_number}<br>GET /api/v1/deals/{deal_number}<br>GET /api/v1/deals/{deal_number}/allowed-payment-methods/<br>POST /api/v1/deals/{deal_number}/action/approve<br>POST /api/v1/dea |
| `deal_party__invalid` | 400 | Canceling party is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/cancel |
| `deal_party__required` | 400 | Canceling party is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/cancel |
| `deal_subject_details__invalid` | 400 | Subject details are not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/<br>PUT /api/v1/deals/{deal_number} |
| `deal_subject_details__required` | 400 | Subject details are required (in case offer_type=service) | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/<br>PUT /api/v1/deals/{deal_number} |
| `delivery_address__required` | 400 | Delivery address is required for the Deal with offer type product | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/submit |
| `district__invalid` | 400 | District is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/billing-address<br>POST /api/v1/deals/{deal_number}/delivery-address |
| `first_name__required` | 400 | First name is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/customers/ |
| `iban__blocked` | 400 | Bank account is blocked | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/account/bank-accounts/ |
| `iban__closed` | 400 | Bank account is closed | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/account/bank-accounts/ |
| `iban__exists` | 400 | IBAN is already exists | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/account/bank-accounts/ |
| `iban__invalid` | 400 | IBAN is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/account/bank-accounts/ |
| `iban__liquidation` | 400 | Bank account is in liquidation | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/account/bank-accounts/ |
| `iban__not_match` | 400 | IBAN is not matching with ID number | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/account/bank-accounts/ |
| `iban__required` | 400 | IBAN is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/account/bank-accounts/ |
| `iban__verification_not_available` | 400 | Bank account verification is not available | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/account/bank-accounts/ |
| `ip__not_whitelisted` | 403 | IP address is not whitelisted | Business rule violation. Surface the message to the operator; do not retry automatically. | DELETE /api/v1/account/bank-accounts/{bank_account_id}<br>DELETE /api/v1/deals/{deal_number}<br>DELETE /api/v1/web-hooks/{webhook_id}<br>GET /api/v1/account<br>GET /api/v1/account/bank-accounts/<br>GE |
| `last_name__required` | 400 | Last name is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/customers/ |
| `offer_category__invalid` | 400 | Offer category is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/<br>PUT /api/v1/deals/{deal_number} |
| `offer_delivery_fee__invalid` | 400 | Offer delivery fee is not valid (in case offer_type=product) | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/<br>PUT /api/v1/deals/{deal_number} |
| `offer_delivery_fee__required` | 400 | Offer delivery fee is required (in case offer_type=product) | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/<br>PUT /api/v1/deals/{deal_number} |
| `offer_description__invalid` | 400 | Offer description is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/<br>PUT /api/v1/deals/{deal_number} |
| `offer_price__invalid` | 400 | Offer price is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/<br>PUT /api/v1/deals/{deal_number} |
| `offer_price__required` | 400 | Offer price is required (in case offer_type=product) | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/<br>PUT /api/v1/deals/{deal_number} |
| `offer_title__invalid` | 400 | Offer title is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/<br>PUT /api/v1/deals/{deal_number} |
| `offer_title__required` | 400 | Offer title is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/<br>PUT /api/v1/deals/{deal_number} |
| `offer_type__invalid` | 400 | Offer type is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/ |
| `offer_type__required` | 400 | Offer type is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/ |
| `page__invalid` | 400 | Page is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | GET /api/v1/customers/<br>GET /api/v1/deals/<br>GET /api/v1/withdrawals/ |
| `page_size__invalid` | 400 | Page size is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | GET /api/v1/withdrawals/ |
| `parties__no_buyer` | 400 | Deal should have at least 1 party with role buyer | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/deals/{deal_number}/action/submit |
| `parties__no_seller` | 400 | Deal should have at least 1 party with role seller | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/deals/{deal_number}/action/submit |
| `party_type__invalid` | 400 | Party type is invalid | Client-side validation: the value is malformed. Fix the request; do not retry. | GET /api/v1/cancel-reasons/ |
| `payment_method__invalid` | 400 | Payment method is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/make-payment-online |
| `payment_method__not_available` | 403 | Wallet payment method is not available for the Deal | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/deals/{deal_number}/action/make-payment-wallet |
| `payment_method__required` | 400 | Payment method is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/make-payment-online |
| `per_page__invalid` | 400 | Per page is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | GET /api/v1/customers/<br>GET /api/v1/deals/ |
| `phone_code__invalid` | 400 | Phone code is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/customers/ |
| `phone_code__required` | 400 | Phone code is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/customers/ |
| `phone_number__invalid` | 400 | Phone number is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/customers/ |
| `phone_number__required` | 400 | Phone number is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/customers/ |
| `price__invalid` | 400 | Price is not valid for Offer with type=service | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/approve |
| `price__required` | 400 | Price is required for Offer with type=service | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/approve |
| `proof_document__invalid` | 400 | Proof document is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/account/bank-accounts/ |
| `proof_document__required` | 400 | Proof document is not provided | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/account/bank-accounts/ |
| `rate_limit__exceeded` | 429 | Rate limit exceeded | Back off exponentially (honour Retry-After if present) and retry. | DELETE /api/v1/deals/{deal_number}<br>GET /api/v1/allowed-country-codes/<br>GET /api/v1/cancel-reasons/<br>GET /api/v1/categories/<br>GET /api/v1/cities<br>GET /api/v1/customers/<br>GET /api/v1/custom |
| `reason__invalid` | 400 | Dispute reason is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/cancel<br>POST /api/v1/deals/{deal_number}/action/dispute<br>POST /api/v1/deals/{deal_number}/action/dispute-approve<br>POST /api/v1/deals/{deal_number}/action/ |
| `reason__required` | 400 | Dispute reason is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/action/cancel<br>POST /api/v1/deals/{deal_number}/action/dispute<br>POST /api/v1/deals/{deal_number}/action/dispute-approve<br>POST /api/v1/deals/{deal_number}/action/ |
| `sellers__invalid` | 400 | Sellers should be a list | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/parties/ |
| `sellers__limit_exceeded` | 400 | Only one seller is allowed for the Deal | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/deals/{deal_number}/parties/ |
| `sellers__required` | 400 | Sellers list is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/parties/ |
| `sellers_idx__not_found` | 400 | Seller (Customer) with specified number does not exist | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/deals/{deal_number}/parties/ |
| `src__invalid` | 400 | Search phrase invalid (min:1, max: 20) | Client-side validation: the value is malformed. Fix the request; do not retry. | GET /api/v1/customers/ |
| `status__invalid` | 400 | Status filter is not invalid | Client-side validation: the value is malformed. Fix the request; do not retry. | GET /api/v1/customers/<br>GET /api/v1/deals/<br>GET /api/v1/withdrawals/ |
| `street__invalid` | 400 | Street is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/billing-address<br>POST /api/v1/deals/{deal_number}/delivery-address |
| `street__required` | 400 | Street is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/billing-address<br>POST /api/v1/deals/{deal_number}/delivery-address |
| `type__invalid` | 400 | Type filter is not invalid | Client-side validation: the value is malformed. Fix the request; do not retry. | GET /api/v1/customers/ |
| `unit_number__invalid` | 400 | Unit number is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/billing-address<br>POST /api/v1/deals/{deal_number}/delivery-address |
| `url__invalid` | 400 | URL is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/web-hooks/ |
| `url__required` | 400 | URL is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/web-hooks/ |
| `webhook__not_found` | 404 | Webhook not found | The referenced object does not exist in this account/environment. Do not retry. | DELETE /api/v1/web-hooks/{webhook_id} |
| `withdrawal__not_allowed` | 400 | Withdrawal is not allowed | Business rule violation. Surface the message to the operator; do not retry automatically. | POST /api/v1/withdrawals/ |
| `withdrawal__not_found` | 404 | Withdrawal not found | The referenced object does not exist in this account/environment. Do not retry. | GET /api/v1/withdrawals/{withdrawal_number} |
| `zip_code__invalid` | 400 | Zip code is not valid | Client-side validation: the value is malformed. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/billing-address<br>POST /api/v1/deals/{deal_number}/delivery-address |
| `zip_code__required` | 400 | Zip code is required | Client-side validation: the field is missing. Fix the request; do not retry. | POST /api/v1/deals/{deal_number}/billing-address<br>POST /api/v1/deals/{deal_number}/delivery-address |

## Generic responses

| HTTP | Meaning | Handling |
|---|---|---|
| 400 | Validation or business-rule error; body lists codes | Fix request / follow lifecycle; never retry blindly |
| 401 | Missing `X-API-Token` | Fix configuration |
| 403 | `api_token__invalid` (wrong token **or wrong environment**), `api_access__disabled` (API not enabled for the account) | Check `AMN_ENV`/token; contact Amen for access |
| 404 | Object not found | Verify identifier |
| 429 | `rate_limit__exceeded` | Exponential backoff with jitter |
| 5xx | Server error | Retry with backoff (max 3), then alert |

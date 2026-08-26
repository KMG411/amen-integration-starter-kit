/** Types mirror openapi/openapi.yml. Money is a string ("100.00"); timestamps are epoch milliseconds. */
export type DealStatus = "draft" | "requested" | "payment_pending" | "paid" | "executing" | "executed"
  | "completed" | "disputed" | "dispute_approved" | "canceled" | "payment_overdue";
export type OfferType = "product" | "service";
export type PaymentMethod = "applepay" | "mada" | "visa_master" | "wallet";
export type DealParty = "buyer" | "seller" | "broker";

export interface Customer { id: string; number: string; first_name: string; last_name: string; status?: string; type?: string; created_at?: number | string; [k: string]: unknown }
export interface Deal { id: string; number: string; status: DealStatus; price?: string; offer?: Record<string, unknown>; buyer?: Customer; seller?: Customer; created_at?: number | string; updated_at?: number | string; [k: string]: unknown }
export interface Checkout { id: number; provider: string; hyperpay?: { checkout_id: string }; amount: string; created_at?: number }
export interface Withdrawal { id: string; number: string; status: string; amount: string; created_at?: number | string; [k: string]: unknown }
export interface Webhook { id: string; url: string; secret_key: string | null }
export interface Account { id?: string; name?: string; wallet?: { balance: string; available: string; escrow: string; on_hold: string; top_up_account?: Record<string, string> }; [k: string]: unknown }
export interface Page<T> { items: T[]; page: number; pages: number; total: number }

export interface CreateCustomer { first_name: string; last_name: string; phone_code: string; phone_number: string }
export interface CreateDeal { offer_type: OfferType; offer_title: string; offer_price?: string; offer_category?: number; offer_description?: string; offer_delivery_fee?: string; deal_subject_details?: string }
export interface Address { city: number; street: string; building_number: string; zip_code: string; district?: string | null; unit_number?: string }

/** Parse an API timestamp: ISO-8601 string (e.g. "2026-08-26T18:04:42.825Z") or epoch-ms number. */
export const toDate = (v?: number | string | null): Date | undefined => (v === null || v === undefined || v === "" ? undefined : new Date(v));

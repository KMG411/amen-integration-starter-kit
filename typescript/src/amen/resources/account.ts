import type { AmenClient } from "../client.js";
import type { Account } from "../types.js";
export class AccountResource {
  constructor(private c: AmenClient) {}
  get() { return this.c.request<Account>("GET", "/account"); }
  bankAccounts() { return this.c.request<Array<{ id: string; iban: string; status: string }>>("GET", "/account/bank-accounts/"); }
  linkBankAccount(iban: string, proofDocument?: Blob) {
    const form = new FormData(); form.set("iban", iban); if (proofDocument) form.set("proof_document", proofDocument);
    return this.c.request<{ id: string }>("POST", "/account/bank-accounts/", { form });
  }
  deleteBankAccount(id: string) { return this.c.request<void>("DELETE", `/account/bank-accounts/${id}`); }
}

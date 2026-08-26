export class AccountResource {
  constructor(c) { this.c = c; }
  get() { return this.c.request("GET", "/account"); }
  bankAccounts() { return this.c.request("GET", "/account/bank-accounts/"); }
  linkBankAccount(iban, proofDocument) { const form = new FormData(); form.set("iban", iban); if (proofDocument) form.set("proof_document", proofDocument); return this.c.request("POST", "/account/bank-accounts/", { form }); }
  deleteBankAccount(id) { return this.c.request("DELETE", `/account/bank-accounts/${id}`); }
}

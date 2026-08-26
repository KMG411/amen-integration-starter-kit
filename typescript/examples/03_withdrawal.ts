import { AmenClient } from "../src/amen/index.js";
const amen = new AmenClient();
console.log("wallet:", (await amen.account.get()).wallet);
const banks = await amen.account.bankAccounts();
if (!banks.length) { console.log("No linked bank account — link one with amen.account.linkBankAccount(iban)."); process.exit(0); }
const w = await amen.withdrawals.create({ bank_account_id: banks[0].id, amount: "10.00" });
console.log(`withdrawal ${w.number}: ${w.status}`);
for (const item of (await amen.withdrawals.list()).items) console.log(" -", item.number, item.status, item.amount);

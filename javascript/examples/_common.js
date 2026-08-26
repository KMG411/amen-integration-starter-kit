export const uniquePhone = (prefix) => `${prefix}${String(Math.floor(Date.now() / 1000)).slice(-7)}`.slice(0, 9);
export const step = (label, deal) => console.log(`✔ ${label}${deal ? ` → status=${deal.status}` : ""}`);

export const uniquePhone = (prefix: string) => `${prefix}${String(Math.floor(Date.now() / 1000)).slice(-7)}`.slice(0, 9);
export const step = (label: string, deal?: { status?: string }) => console.log(`✔ ${label}${deal ? ` → status=${deal.status}` : ""}`);

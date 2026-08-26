/** Payloads are used as plain objects (see openapi/openapi.yml). Money is a string ("100.00"); timestamps are epoch ms. */
export const toDate = (ms) => (ms ? new Date(ms) : undefined);
export function toPage(d, key) {
  const p = d && typeof d.page === "object" ? d.page : d ?? {};
  return { items: d?.[key] ?? d?.results ?? d?.items ?? [], page: p.page ?? 0, pages: p.pages ?? 1, total: p.total ?? 0 };
}

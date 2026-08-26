/** Payloads are used as plain objects (see openapi/openapi.yml). Money is a string ("100.00"); timestamps are epoch ms. */
/** Parse an API timestamp: ISO-8601 string (e.g. "2026-08-26T18:04:42.825Z") or epoch-ms number. */
export const toDate = (v) => (v === null || v === undefined || v === "" ? undefined : new Date(v));
export function toPage(d, key) {
  const p = d && typeof d.page === "object" ? d.page : d ?? {};
  return { items: d?.[key] ?? d?.results ?? d?.items ?? [], page: p.page ?? 0, pages: p.pages ?? 1, total: p.total ?? 0 };
}

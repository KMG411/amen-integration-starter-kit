import type { Page } from "../types.js";
export function toPage<T>(d: any, key: string): Page<T> {
  const p = d && typeof d.page === "object" ? d.page : d ?? {};
  return { items: d?.[key] ?? d?.results ?? d?.items ?? [], page: p.page ?? 0, pages: p.pages ?? 1, total: p.total ?? 0 };
}

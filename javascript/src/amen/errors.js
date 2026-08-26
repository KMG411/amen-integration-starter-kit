/** Any non-2xx response. `codes` holds the API's error codes, e.g. "price__required". */
export class AmenApiError extends Error {
  constructor(status, codes, method, path, body) {
    super(`${status} ${method} ${path}: ${codes.join(", ") || JSON.stringify(body)}`);
    this.name = "AmenApiError"; Object.assign(this, { status, codes, method, path, body });
  }
  has(code) { return this.codes.includes(code); }
  get retryable() { return this.status === 429 || this.status >= 500; }
}
/** Raised locally, before any HTTP call, when an action is not valid for the deal's status. */
export class AmenLifecycleError extends Error { constructor(m) { super(m); this.name = "AmenLifecycleError"; } }

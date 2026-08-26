/** Any non-2xx response. `codes` holds the API's error codes, e.g. "price__required". */
export class AmenApiError extends Error {
  constructor(public status: number, public codes: string[], public method: string, public path: string, public body?: unknown) {
    super(`${status} ${method} ${path}: ${codes.join(", ") || JSON.stringify(body)}`);
    this.name = "AmenApiError";
  }
  has(code: string): boolean { return this.codes.includes(code); }
  get retryable(): boolean { return this.status === 429 || this.status >= 500; }
}

/** Raised locally, before any HTTP call, when an action is not valid for the deal's status. */
export class AmenLifecycleError extends Error {
  constructor(message: string) { super(message); this.name = "AmenLifecycleError"; }
}

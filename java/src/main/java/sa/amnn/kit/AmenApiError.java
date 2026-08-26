package sa.amnn.kit;

import java.util.List;

/** Any non-2xx response. {@code codes} holds the API's error codes, e.g. "price__required". */
public class AmenApiError extends RuntimeException {
    public final int status; public final List<String> codes; public final String method, path, body;
    public AmenApiError(int status, List<String> codes, String method, String path, String body) {
        super(status + " " + method + " " + path + ": " + (codes.isEmpty() ? body : String.join(", ", codes)));
        this.status = status; this.codes = codes; this.method = method; this.path = path; this.body = body;
    }
    public boolean has(String code) { return codes.contains(code); }
    public boolean isRetryable() { return status == 429 || status >= 500; }
}

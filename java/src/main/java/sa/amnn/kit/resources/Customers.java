package sa.amnn.kit.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import sa.amnn.kit.AmenClient;
import sa.amnn.kit.Models.CreateCustomer;
import sa.amnn.kit.Models.Customer;
import sa.amnn.kit.Models.Page;

public final class Customers {
    private final AmenClient c; public Customers(AmenClient c) { this.c = c; }
    public Customer create(CreateCustomer body) { return c.post("/customers/", body, new TypeReference<>() {}); }
    public Customer get(String customerNumber) { return c.get("/customers/" + customerNumber, Map.of(), new TypeReference<>() {}); }
    public Page<Customer> list(Map<String, String> params) { return c.page("/customers/", params, "customers", Customer.class); }
    /** Iterate every page — never process only the first page by accident. */
    public Iterable<Customer> all(Map<String, String> filters) {
        return () -> new Iterator<Customer>() {
            int page = 0; Page<Customer> cur = fetch(); int i = 0;
            private Page<Customer> fetch() { Map<String, String> p = new HashMap<>(filters); p.put("page", String.valueOf(page)); return list(p); }
            public boolean hasNext() { if (i < cur.items().size()) return true; if (page + 1 >= cur.pages() || cur.items().isEmpty()) return false; page++; cur = fetch(); i = 0; return !cur.items().isEmpty(); }
            public Customer next() { return cur.items().get(i++); }
        };
    }
}

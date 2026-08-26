package sa.amnn.kit.resources;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import sa.amnn.kit.AmenClient;
import sa.amnn.kit.Models.Lookup;

public final class Lookups {
    private static final TypeReference<List<Lookup>> LIST = new TypeReference<>() {};
    private final AmenClient c; public Lookups(AmenClient c) { this.c = c; }
    public List<Lookup> cities() { return c.get("/cities", Map.of(), LIST); }
    public List<Lookup> categories() { return c.get("/categories/", Map.of(), LIST); }
    public List<Lookup> disputeReasons() { return c.get("/dispute-reasons/", Map.of(), LIST); }
    public List<Lookup> disputeResolutionReasons() { return c.get("/dispute-resolution-reasons/", Map.of(), LIST); }
    public List<Lookup> cancelReasons(String partyType) { Map<String, String> p = new HashMap<>(); p.put("party_type", partyType); return c.get("/cancel-reasons/", p, LIST); }
}

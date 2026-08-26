package sa.amnn.kit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Models mirror openapi/openapi.yml. Money is a String ("100.00"); timestamps are epoch milliseconds. Unknown fields are ignored. */
public final class Models {
    private Models() {}
    public static final ObjectMapper JSON = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /** Parse an API timestamp: ISO-8601 string (e.g. "2026-08-26T18:04:42.825Z"). */
    public static Instant toInstant(String iso) { try { return iso == null || iso.isBlank() ? null : Instant.parse(iso); } catch (Exception e) { return null; } }

    @JsonIgnoreProperties(ignoreUnknown = true) public record Customer(String id, String number, String firstName, String lastName, String status, String createdAt) {}
    @JsonIgnoreProperties(ignoreUnknown = true) public record Deal(String id, String number, String status, String price, String createdAt, String updatedAt) {}
    @JsonIgnoreProperties(ignoreUnknown = true) public record Checkout(Integer id, String provider, Map<String, Object> hyperpay, String amount) {}
    @JsonIgnoreProperties(ignoreUnknown = true) public record Withdrawal(String id, String number, String status, String amount) {}
    /** secretKey is returned ONLY at creation — store it in a secret manager immediately. */
    @JsonIgnoreProperties(ignoreUnknown = true) public record Webhook(String id, String url, String secretKey) {}
    @JsonIgnoreProperties(ignoreUnknown = true) public record Account(String id, String name, Map<String, Object> wallet) {}
    @JsonIgnoreProperties(ignoreUnknown = true) public record Lookup(int id, String name) {}
    @JsonIgnoreProperties(ignoreUnknown = true) public record BankAccount(String id, String iban, String status) {}
    public record Page<T>(List<T> items, int page, int pages, int total) {}

    public record CreateCustomer(String firstName, String lastName, String phoneCode, String phoneNumber) {}
    public static final class CreateDeal {
        @JsonProperty("offer_type") public final String offerType; @JsonProperty("offer_title") public final String offerTitle;
        @JsonProperty("offer_price") public String offerPrice; @JsonProperty("offer_delivery_fee") public String offerDeliveryFee;
        @JsonProperty("offer_category") public Integer offerCategory; @JsonProperty("offer_description") public String offerDescription;
        @JsonProperty("deal_subject_details") public String dealSubjectDetails;
        public CreateDeal(String offerType, String offerTitle) { this.offerType = offerType; this.offerTitle = offerTitle; }
        public CreateDeal price(String v) { offerPrice = v; return this; }
        public CreateDeal deliveryFee(String v) { offerDeliveryFee = v; return this; }
        public CreateDeal category(int v) { offerCategory = v; return this; }
        public CreateDeal description(String v) { offerDescription = v; return this; }
        public CreateDeal subjectDetails(String v) { dealSubjectDetails = v; return this; }
    }
    public record Address(int city, String street, String buildingNumber, String zipCode, String district, String unitNumber) {}
}

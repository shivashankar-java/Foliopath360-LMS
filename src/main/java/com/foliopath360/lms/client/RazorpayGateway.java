package com.foliopath360.lms.client;

import com.foliopath360.lms.config.RazorpayProperties;
import com.foliopath360.lms.exception.RazorpayApiException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

/**
 * Thin HTTP client for the Razorpay Orders API.
 *
 * Only the operations the LMS needs are exposed; signatures are verified
 * locally via {@link com.foliopath360.lms.util.RazorpaySignatureUtil}.
 */
@Component
public class RazorpayGateway {

    private static final String BASE_URL = "https://api.razorpay.com/v1";

    private final RazorpayProperties properties;
    private final RestClient restClient;

    // Plain Jackson instance for parsing Razorpay API responses.
    // (Boot 4 auto-configures Jackson 3, so we do not inject this as a bean.)
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RazorpayGateway(RazorpayProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory(requestFactory)
                .defaultHeaders(headers ->
                        headers.setBasicAuth(
                                properties.getKeyId() == null ? "" : properties.getKeyId(),
                                properties.getKeySecret() == null ? "" : properties.getKeySecret()))
                .build();
    }

    /**
     * Creates a Razorpay order and returns its id (format: order_xxxxxxxx).
     *
     * @param amountInPaise amount in paise (Rs.9,999 -> 999900)
     * @param currency      e.g. INR
     * @param receipt       our internal reference (order number)
     */
    public String createOrder(long amountInPaise, String currency, String receipt) {

        requireConfigured();

        Map<String, Object> body = Map.of(
                "amount", amountInPaise,
                "currency", currency,
                "receipt", receipt
        );

        try {
            String response = restClient.post()
                    .uri("/orders")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            JsonNode node = objectMapper.readTree(response);
            String razorpayOrderId = node.path("id").asText(null);

            if (razorpayOrderId == null || razorpayOrderId.isBlank()) {
                throw new RazorpayApiException(
                        "Unexpected response from Razorpay while creating order");
            }
            return razorpayOrderId;

        } catch (RestClientResponseException ex) {
            throw new RazorpayApiException(
                    "Razorpay rejected the order request: "
                            + ex.getResponseBodyAsString(), ex);
        } catch (ResourceAccessException ex) {
            throw new RazorpayApiException(
                    "Could not reach Razorpay. Please try again.", ex);
        } catch (JsonProcessingException ex) {
            throw new RazorpayApiException(
                    "Failed to parse the Razorpay response", ex);
        }
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            throw new IllegalStateException(
                    "Razorpay credentials are not configured. Set RAZORPAY_KEY_ID "
                            + "and RAZORPAY_KEY_SECRET environment variables.");
        }
    }
}

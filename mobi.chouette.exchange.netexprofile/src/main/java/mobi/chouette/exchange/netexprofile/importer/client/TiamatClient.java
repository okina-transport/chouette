package mobi.chouette.exchange.netexprofile.importer.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j;
import mobi.chouette.exchange.importer.utils.TokenService;
import org.apache.commons.lang3.CharEncoding;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.util.Set;

@Log4j
public class TiamatClient {

    public static final String PROPERTY_TIAMAT_BASE_URL = System.getenv("TIAMAT_BASE_URL");
    private static final String QUAY_GEOCODE_RESOURCE = "netex_stops/geocode";

    private static String buildRequestBody(Set<String> netexIdentifiers) {
        String jsonArrayString = "[]";
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            jsonArrayString = objectMapper.writeValueAsString(netexIdentifiers);
        } catch (JsonProcessingException e) {
            log.error("Error converting netex identifiers to JSON", e);
        }
        return jsonArrayString;
    }

    public void sendQuayNetexIdForGeocoding(Set<String> netexIdentifiers) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost postRequest = new HttpPost(PROPERTY_TIAMAT_BASE_URL + QUAY_GEOCODE_RESOURCE);
            postRequest.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            postRequest.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            TokenService tokenService = mobi.chouette.exchange.utils.TokenServiceBuilder.init().build();
            postRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getToken());

            String jsonArrayString = buildRequestBody(netexIdentifiers);
            StringEntity entity = new StringEntity(jsonArrayString);
            entity.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            entity.setContentEncoding(CharEncoding.UTF_8);
            postRequest.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(postRequest)) {
                int httpResponseCode = response.getStatusLine().getStatusCode();
                if (httpResponseCode != HttpStatus.SC_OK) {
                    log.error("Error sending stop id for geocoding. Response code: " + httpResponseCode);
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
    }
}

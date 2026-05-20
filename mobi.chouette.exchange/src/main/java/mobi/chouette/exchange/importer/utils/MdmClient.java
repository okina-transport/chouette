package mobi.chouette.exchange.importer.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.rutebanken.netex.client.TokenService;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static mobi.chouette.common.Constant.*;

@Log4j
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton(name = "MdmClient")
public class MdmClient {

    private static final String MDM_URL = System.getProperty("iev.mdm.url");

    private static final String QUAYS_RESOURCE = "quays/super-identifiers";

    private static final String STOPS_RESOURCE = "stops/super-identifiers";

    private final ObjectMapper mapper = new ObjectMapper();

    private TokenService tokenService;

    public Map<String, String> getQuayImportedId(Set<String> superId) {
        return getIdentifiers(QUAYS_RESOURCE, superId, QUAY);
    }

    public Map<String, String> getStopImportedId(Set<String> superId) {
        return getIdentifiers(STOPS_RESOURCE, superId, STOP_PLACE);
    }

    private @NonNull Map<String, String> getIdentifiers(String targetResource, Set<String> superId, String type) {
        Map<String, String> importedIdSuperIdMapping = new HashMap<>();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            URIBuilder uriBuilder = new URIBuilder(StringUtils.appendIfMissing(MDM_URL, "/") + targetResource);
            superId.forEach(quayId -> uriBuilder.addParameter("ids", quayId));
            HttpGet getRequest = new HttpGet(uriBuilder.build());
            getRequest.setHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
            getRequest.setHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());
            if (tokenService == null) {
                tokenService = mobi.chouette.exchange.utils.TokenServiceBuilder.init().build();
            }
            getRequest.setHeader(HttpHeaders.AUTHORIZATION, "Bearer " + tokenService.getToken());

            try (CloseableHttpResponse response = httpClient.execute(getRequest)) {
                int httpResponseCode = response.getStatusLine().getStatusCode();
                if (httpResponseCode != HttpStatus.SC_OK) {
                    log.error("Error retrieving imported id from MDM. Response code: " + httpResponseCode);
                } else {
                    HttpEntity entity = response.getEntity();

                    if (entity != null) {
                        String json = EntityUtils.toString(entity);
                        List<mobi.chouette.model.util.OkinaIdentifier> identifiers = mapper.readValue(
                                json,
                                new TypeReference<List<mobi.chouette.model.util.OkinaIdentifier>>() {
                                }
                        );
                        for (mobi.chouette.model.util.OkinaIdentifier identifier : identifiers) {
                            importedIdSuperIdMapping.put(
                                    identifier.getDataset() + type + identifier.getOriginalId(),
                                    StringUtils.upperCase(SUPERSPACE_PREFIX) + type + identifier.getSuperId()
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
        return importedIdSuperIdMapping;
    }

}

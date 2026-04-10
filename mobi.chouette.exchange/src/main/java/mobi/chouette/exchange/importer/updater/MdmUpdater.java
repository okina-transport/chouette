package mobi.chouette.exchange.importer.updater;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j;
import mobi.chouette.common.ContenerChecker;
import mobi.chouette.model.*;
import org.apache.commons.lang3.StringUtils;
import mobi.chouette.exchange.importer.utils.TokenService;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;


import static mobi.chouette.common.PropertyNames.*;
import static mobi.chouette.common.PropertyNames.KC_CLIENT_AUTH_URL;
import static mobi.chouette.common.PropertyNames.KC_CLIENT_REALM;


@Log4j
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@Singleton(name = MdmUpdater.BEAN_NAME)
public class MdmUpdater {

    public static final String BEAN_NAME = "MdmUpdater";

    private static final String CHOUETTE_UPDATE_RESOURCE = "chouette/updateImportedIds";

    @EJB
    private ContenerChecker contenerChecker;


    private TokenService tokenService;

    public void sendDataToMdm(ChouetteData chouetteData){

        String mdmUrl = getAndValidateProperty(MDM_URL);
        String clientId = getAndValidateProperty(KC_CLIENT_ID);
        String clientSecret = getAndValidateProperty(KC_CLIENT_SECRET);
        String realm = getAndValidateProperty(KC_CLIENT_REALM);
        String authServerUrl = getAndValidateProperty(KC_CLIENT_AUTH_URL);

        if (tokenService == null) {
            tokenService = new TokenService(clientId, clientSecret, realm, authServerUrl);
        }
        HttpURLConnection connection = null;

        try {
            URL url = new URL(StringUtils.appendIfMissing(mdmUrl, "/") + CHOUETTE_UPDATE_RESOURCE);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-type", "application/json");
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + this.tokenService.getToken());
            log.info("Sending data to mdm");
            ObjectMapper objectMapper = new ObjectMapper();
            String body = objectMapper.writeValueAsString(chouetteData);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = body.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            int responseCode = connection.getResponseCode();
            log.info("Data sent to MDM return:"  + responseCode);
        } catch (Exception e) {
            log.error("Error posting data to MDM", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String getAndValidateProperty(String propertyName) {
        String urlPropertyKey = contenerChecker.getContext() + propertyName;
        String propertyValue = System.getProperty(urlPropertyKey);
        if (propertyValue == null) {
            log.warn("Cannot read property " + urlPropertyKey + ". Will not update stop place registry.");
        }
        return propertyValue;
    }


}

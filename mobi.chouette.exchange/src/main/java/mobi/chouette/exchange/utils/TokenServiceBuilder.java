package mobi.chouette.exchange.utils;

import org.rutebanken.netex.client.TokenService;

import static mobi.chouette.common.PropertyNames.*;

public class TokenServiceBuilder {

    private static final String IEV_PREFIX = "iev";

    private final TokenService tokenService;

    TokenServiceBuilder() {
        String clientId = System.getProperty(IEV_PREFIX + KC_CLIENT_ID);
        String clientSecret = System.getProperty(IEV_PREFIX + KC_CLIENT_SECRET);
        String realm = System.getProperty(IEV_PREFIX + KC_CLIENT_REALM);
        String authServerUrl = System.getProperty(IEV_PREFIX + KC_CLIENT_AUTH_URL);
        tokenService = new TokenService(clientId, clientSecret, realm, authServerUrl);
    }

    public static TokenServiceBuilder init() {
        return new TokenServiceBuilder();
    }

    public TokenService build() {
        return tokenService;
    }
}

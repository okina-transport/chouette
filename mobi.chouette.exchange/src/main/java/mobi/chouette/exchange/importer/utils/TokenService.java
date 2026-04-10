package mobi.chouette.exchange.importer.utils;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

public class TokenService {
    private final Keycloak keycloakClient;

    public TokenService(String clientId, String clientSecret, String realm, String authServerUrl) {
        this.keycloakClient = KeycloakBuilder.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .realm(realm)
                .serverUrl(authServerUrl)
                .grantType("client_credentials")
                .build();
    }

    public String getToken() {
        return this.keycloakClient.tokenManager().getAccessTokenString();
    }
}

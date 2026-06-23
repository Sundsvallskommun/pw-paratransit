package se.sundsvall.paratransit.integration.operaton.configuration;

import org.camunda.bpm.client.interceptor.ClientRequestContext;
import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;

import static java.util.Objects.isNull;
import static se.sundsvall.paratransit.integration.operaton.configuration.OperatonConfiguration.CLIENT_ID;

/**
 * Adds a WSO2 client-credentials bearer token to every external task request (fetchAndLock/complete/handleFailure) so
 * the Operaton instance can poll the engine, which sits behind the OAuth2-secured gateway. Only registered when this
 * instance targets Operaton (see {@link OperatonExternalTaskClientConfiguration}); the Camunda instance polls its
 * engine
 * directly without a token.
 * <p>
 * An interceptor never sees the response, so it cannot detect a 401 and evict a stale token afterwards (unlike the
 * Feign
 * path). To stay robust against tokens that WSO2 invalidates server-side before their nominal expiry (gateway restart,
 * revocation, clock drift), every poll evicts the cached authorized client before authorizing, forcing the manager to
 * obtain a freshly issued token instead of reusing the cached one.
 */
class OperatonExternalTaskAuthInterceptor implements ClientRequestInterceptor {

	private static final String PRINCIPAL = "operaton-external-task-client";

	private final OAuth2AuthorizedClientManager authorizedClientManager;
	private final OAuth2AuthorizedClientService authorizedClientService;

	OperatonExternalTaskAuthInterceptor(final OAuth2AuthorizedClientManager authorizedClientManager, final OAuth2AuthorizedClientService authorizedClientService) {
		this.authorizedClientManager = authorizedClientManager;
		this.authorizedClientService = authorizedClientService;
	}

	@Override
	public void intercept(final ClientRequestContext requestContext) {
		authorizedClientService.removeAuthorizedClient(CLIENT_ID, PRINCIPAL);

		final var authorizedClient = authorizedClientManager.authorize(
			OAuth2AuthorizeRequest.withClientRegistrationId(CLIENT_ID).principal(PRINCIPAL).build());

		if (isNull(authorizedClient)) {
			throw new IllegalStateException("Could not obtain a WSO2 access token for client registration '" + CLIENT_ID + "'; external task polling cannot authenticate against the Operaton gateway");
		}

		requestContext.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + authorizedClient.getAccessToken().getTokenValue());
	}
}

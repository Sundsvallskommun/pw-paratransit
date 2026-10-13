package se.sundsvall.paratransit.integration.engine.externaltask;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.camunda.bpm.client.exception.ConnectionLostException;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.exception.UnknownHttpErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.oauth2.core.AuthorizationGrantType.CLIENT_CREDENTIALS;

class OperatonExternalTaskTokenTest {

	private final List<ClientRegistration> requestedRegistrations = new ArrayList<>();

	private OperatonExternalTaskToken token;

	@BeforeEach
	void setUp() {
		final var registration = ClientRegistration.withRegistrationId("operaton")
			.clientId("the-client-id")
			.clientSecret("the-client-secret")
			.authorizationGrantType(CLIENT_CREDENTIALS)
			.tokenUri("http://localhost/token")
			.scope("openid")
			.build();
		token = new OperatonExternalTaskToken(new InMemoryClientRegistrationRepository(registration));

		// Stands in for the token endpoint: issues a new token unless one is already cached, as the client credentials
		// provider does for a token that has not expired
		setAuthorizedClientProvider(context -> {
			if (context.getAuthorizedClient() != null) {
				return null;
			}
			requestedRegistrations.add(context.getClientRegistration());
			final var issuedAt = Instant.now();
			final var accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "token-" + requestedRegistrations.size(), issuedAt, issuedAt.plusSeconds(3600));
			return new OAuth2AuthorizedClient(context.getClientRegistration(), context.getPrincipal().getName(), accessToken);
		});
	}

	@Test
	void reusesCachedTokenUntilEvicted() {
		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-1");
		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-1");

		token.evict();

		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-2");
		assertThat(requestedRegistrations).hasSize(2);
	}

	@Test
	void evictBeforeAnyTokenIsNoop() {
		token.evict();

		assertThat(token.getAuthorizationHeader()).isEqualTo("Bearer token-1");
		assertThat(requestedRegistrations).hasSize(1);
	}

	@Test
	void requestsTokenWithDeviceScope() {
		token.getAuthorizationHeader();

		assertThat(requestedRegistrations.getFirst().getRegistrationId()).isEqualTo("operaton");
		assertThat(requestedRegistrations.getFirst().getScopes())
			.hasSize(2)
			.contains("openid")
			.anyMatch(scope -> scope.startsWith("device_"));
	}

	@Test
	void isUnauthorized() {
		assertThat(OperatonExternalTaskToken.isUnauthorized(new UnknownHttpErrorException("Unauthorized", restException(401)))).isTrue();
		assertThat(OperatonExternalTaskToken.isUnauthorized(restException(401))).isTrue();
		assertThat(OperatonExternalTaskToken.isUnauthorized(new EngineException("Internal error", restException(500)))).isFalse();
		assertThat(OperatonExternalTaskToken.isUnauthorized(new RestException("No status", "type", 0))).isFalse();
		assertThat(OperatonExternalTaskToken.isUnauthorized(new ConnectionLostException("Connection lost", new IOException()))).isFalse();
		assertThat(OperatonExternalTaskToken.isUnauthorized(null)).isFalse();
	}

	private void setAuthorizedClientProvider(final OAuth2AuthorizedClientProvider provider) {
		final var oAuth2RequestInterceptor = ReflectionTestUtils.getField(token, "oAuth2RequestInterceptor");
		final var manager = (AuthorizedClientServiceOAuth2AuthorizedClientManager) ReflectionTestUtils.getField(oAuth2RequestInterceptor, "authorizedClientManager");
		manager.setAuthorizedClientProvider(provider);
	}

	private static RestException restException(final int httpStatusCode) {
		final var restException = new RestException("message", "type", 0);
		restException.setHttpStatusCode(httpStatusCode);
		return restException;
	}
}

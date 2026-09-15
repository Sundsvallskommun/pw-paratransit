package se.sundsvall.paratransit.integration.operaton.configuration;

import java.time.Instant;
import org.camunda.bpm.client.interceptor.ClientRequestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatonExternalTaskAuthInterceptorTest {

	private static final String CLIENT_ID = "operaton";
	private static final String PRINCIPAL = "operaton-external-task-client";

	@Mock
	private OAuth2AuthorizedClientManager authorizedClientManagerMock;

	@Mock
	private OAuth2AuthorizedClientService authorizedClientServiceMock;

	@Mock
	private ClientRequestContext requestContextMock;

	@Test
	void addsBearerTokenHeader() {
		final var issuedAt = Instant.parse("2026-01-01T00:00:00Z");
		final var accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "the-token", issuedAt, issuedAt.plusSeconds(60));
		final var authorizedClient = mock(OAuth2AuthorizedClient.class);
		when(authorizedClient.getAccessToken()).thenReturn(accessToken);
		when(authorizedClientManagerMock.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient);

		new OperatonExternalTaskAuthInterceptor(authorizedClientManagerMock, authorizedClientServiceMock).intercept(requestContextMock);

		verify(requestContextMock).addHeader("Authorization", "Bearer the-token");
	}

	@Test
	void evictsCachedTokenBeforeEveryAuthorize() {
		final var issuedAt = Instant.parse("2026-01-01T00:00:00Z");
		final var accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "the-token", issuedAt, issuedAt.plusSeconds(60));
		final var authorizedClient = mock(OAuth2AuthorizedClient.class);
		when(authorizedClient.getAccessToken()).thenReturn(accessToken);
		when(authorizedClientManagerMock.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(authorizedClient);

		final var interceptor = new OperatonExternalTaskAuthInterceptor(authorizedClientManagerMock, authorizedClientServiceMock);
		interceptor.intercept(requestContextMock);
		interceptor.intercept(requestContextMock);

		final InOrder inOrder = inOrder(authorizedClientServiceMock, authorizedClientManagerMock);
		inOrder.verify(authorizedClientServiceMock).removeAuthorizedClient(CLIENT_ID, PRINCIPAL);
		inOrder.verify(authorizedClientManagerMock).authorize(any(OAuth2AuthorizeRequest.class));
		inOrder.verify(authorizedClientServiceMock).removeAuthorizedClient(CLIENT_ID, PRINCIPAL);
		inOrder.verify(authorizedClientManagerMock).authorize(any(OAuth2AuthorizeRequest.class));
	}

	@Test
	void throwsWhenNoTokenCanBeObtained() {
		when(authorizedClientManagerMock.authorize(any(OAuth2AuthorizeRequest.class))).thenReturn(null);

		final var interceptor = new OperatonExternalTaskAuthInterceptor(authorizedClientManagerMock, authorizedClientServiceMock);

		assertThatExceptionOfType(IllegalStateException.class)
			.isThrownBy(() -> interceptor.intercept(requestContextMock));

		verifyNoInteractions(requestContextMock);
	}
}

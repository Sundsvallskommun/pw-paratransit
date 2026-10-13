package se.sundsvall.paratransit.integration.engine.externaltask;

import feign.RequestTemplate;
import java.util.Set;
import org.camunda.bpm.client.exception.RestException;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Component;
import se.sundsvall.dept44.configuration.feign.interceptor.OAuth2RequestInterceptor;

import static java.util.UUID.randomUUID;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static se.sundsvall.paratransit.integration.operaton.configuration.OperatonConfiguration.CLIENT_ID;

/**
 * WSO2 token for the external task client, with a per-instance {@code device_} scope so that replicas get separate
 * tokens and cannot invalidate each other's (HYDRAN-2566). Token handling is delegated to dept44's Feign interceptor.
 */
@Component
public class OperatonExternalTaskToken {

	private final OAuth2RequestInterceptor oAuth2RequestInterceptor;

	private volatile String lastAuthorizationHeader;

	OperatonExternalTaskToken(final ClientRegistrationRepository clientRegistrationRepository) {
		this.oAuth2RequestInterceptor = new OAuth2RequestInterceptor(clientRegistrationRepository.findByRegistrationId(CLIENT_ID), Set.of("device_" + randomUUID()));
	}

	static boolean isUnauthorized(final Throwable throwable) {
		return throwable instanceof final RestException restException && Integer.valueOf(UNAUTHORIZED.value()).equals(restException.getHttpStatusCode());
	}

	String getAuthorizationHeader() {
		final var requestTemplate = new RequestTemplate();
		oAuth2RequestInterceptor.apply(requestTemplate);
		lastAuthorizationHeader = requestTemplate.headers().get(AUTHORIZATION).iterator().next();
		return lastAuthorizationHeader;
	}

	// Why: only evicts if the cached token is still the one last handed out, so a token fetched in between survives
	void evict() {
		oAuth2RequestInterceptor.removeToken(lastAuthorizationHeader);
	}
}

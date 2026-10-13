package se.sundsvall.paratransit.integration.engine.externaltask;

import org.camunda.bpm.client.interceptor.ClientRequestContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperatonExternalTaskAuthInterceptorTest {

	@Mock
	private OperatonExternalTaskToken tokenMock;

	@Mock
	private ClientRequestContext requestContextMock;

	@Test
	void addsBearerTokenHeader() {
		when(tokenMock.getAuthorizationHeader()).thenReturn("Bearer the-token");

		new OperatonExternalTaskAuthInterceptor(tokenMock).intercept(requestContextMock);

		verify(requestContextMock).addHeader("Authorization", "Bearer the-token");
	}
}

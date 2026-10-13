package se.sundsvall.paratransit.integration.engine.externaltask;

import java.io.IOException;
import java.util.List;
import org.camunda.bpm.client.exception.ConnectionLostException;
import org.camunda.bpm.client.exception.EngineException;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.exception.UnknownHttpErrorException;
import org.camunda.bpm.client.task.ExternalTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class TokenEvictingBackoffStrategyTest {

	@Mock
	private OperatonExternalTaskToken tokenMock;

	private TokenEvictingBackoffStrategy strategy;

	@BeforeEach
	void setUp() {
		strategy = new TokenEvictingBackoffStrategy(500, 2, 15000, tokenMock);
	}

	@Test
	void evictsTokenAndBacksOffOnUnauthorized() {
		strategy.reconfigure(List.of(), new UnknownHttpErrorException("Unauthorized", restException(401)));

		verify(tokenMock).evict();
		assertThat(strategy.calculateBackoffTime()).isEqualTo(500);
	}

	@Test
	void backsOffWithoutEvictionOnServerError() {
		strategy.reconfigure(List.of(), new EngineException("Internal error", restException(500)));

		verifyNoInteractions(tokenMock);
		assertThat(strategy.calculateBackoffTime()).isEqualTo(500);
	}

	@Test
	void backsOffWithoutEvictionOnConnectionLost() {
		strategy.reconfigure(List.of(), new ConnectionLostException("Connection lost", new IOException()));

		verifyNoInteractions(tokenMock);
		assertThat(strategy.calculateBackoffTime()).isEqualTo(500);
	}

	@Test
	void increasesBackoffOnRepeatedErrors() {
		strategy.reconfigure(List.of(), new ConnectionLostException("Connection lost", new IOException()));
		strategy.reconfigure(List.of(), new ConnectionLostException("Connection lost", new IOException()));

		assertThat(strategy.calculateBackoffTime()).isEqualTo(1000);
	}

	@Test
	void noBackoffOnEmptyPollWithoutError() {
		strategy.reconfigure(List.of(), null);

		verifyNoInteractions(tokenMock);
		assertThat(strategy.calculateBackoffTime()).isZero();
	}

	@Test
	void resetsBackoffWhenTasksAreFetched() {
		strategy.reconfigure(List.of(), new ConnectionLostException("Connection lost", new IOException()));
		strategy.reconfigure(List.of(mock(ExternalTask.class)), null);

		assertThat(strategy.calculateBackoffTime()).isZero();
	}

	private static RestException restException(final int httpStatusCode) {
		final var restException = new RestException("message", "type", 0);
		restException.setHttpStatusCode(httpStatusCode);
		return restException;
	}
}

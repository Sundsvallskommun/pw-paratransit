package se.sundsvall.paratransit.integration.engine.externaltask;

import org.camunda.bpm.client.backoff.BackoffStrategy;
import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ExternalTaskClientConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withUserConfiguration(ExternalTaskClientConfiguration.class)
		.withBean(BackoffProperties.class, () -> new BackoffProperties(500, 2, 15000))
		.withBean(OperatonExternalTaskToken.class, () -> mock(OperatonExternalTaskToken.class));

	@Test
	void interceptorRegisteredWhenEngineIsOperaton() {
		contextRunner.withPropertyValues("process-engine.type=operaton")
			.run(context -> assertThat(context).hasSingleBean(ClientRequestInterceptor.class));
	}

	@Test
	void interceptorNotRegisteredWhenEngineIsCamunda() {
		contextRunner.withPropertyValues("process-engine.type=camunda")
			.run(context -> assertThat(context).doesNotHaveBean(ClientRequestInterceptor.class));
	}

	@Test
	void interceptorNotRegisteredByDefault() {
		contextRunner.run(context -> assertThat(context).doesNotHaveBean(ClientRequestInterceptor.class));
	}

	@Test
	void tokenEvictingBackoffStrategyRegisteredForBothEngines() {
		contextRunner.withPropertyValues("process-engine.type=camunda")
			.run(context -> assertThat(context).getBean(BackoffStrategy.class).isInstanceOf(TokenEvictingBackoffStrategy.class));
		contextRunner.withPropertyValues("process-engine.type=operaton")
			.run(context -> assertThat(context).getBean(BackoffStrategy.class).isInstanceOf(TokenEvictingBackoffStrategy.class));
	}
}

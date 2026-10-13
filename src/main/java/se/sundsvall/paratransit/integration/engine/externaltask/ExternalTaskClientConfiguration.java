package se.sundsvall.paratransit.integration.engine.externaltask;

import org.camunda.bpm.client.backoff.BackoffStrategy;
import org.camunda.bpm.client.interceptor.ClientRequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ExternalTaskClientConfiguration {

	// Why: error-aware, so an empty long poll is followed by the next one at once. Wired for both engine types; a
	// Camunda engine polled directly never answers 401, so the eviction never triggers there.
	@Bean
	@Primary
	BackoffStrategy backoffStrategy(final BackoffProperties properties, final OperatonExternalTaskToken token) {
		return new TokenEvictingBackoffStrategy(properties.initTime(), properties.factor(), properties.maxTime(), token);
	}

	// Why: the client registers every interceptor bean, so a Camunda instance must not have this one
	@Bean
	@ConditionalOnProperty(name = "process-engine.type", havingValue = "operaton")
	ClientRequestInterceptor operatonExternalTaskAuthInterceptor(final OperatonExternalTaskToken token) {
		return new OperatonExternalTaskAuthInterceptor(token);
	}
}

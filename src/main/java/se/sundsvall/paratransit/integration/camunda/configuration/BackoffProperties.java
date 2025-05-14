package se.sundsvall.paratransit.integration.camunda.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("camunda.bpm.client.backoff")
public record BackoffProperties(long initTime, float factor, long maxTime) {
}

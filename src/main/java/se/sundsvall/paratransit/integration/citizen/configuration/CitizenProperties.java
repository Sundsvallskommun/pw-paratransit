package se.sundsvall.paratransit.integration.citizen.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("integration.citizen")
public record CitizenProperties(int connectTimeout, int readTimeout) {}

package se.sundsvall.paratransit.integration.citizen;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.paratransit.integration.citizen.configuration.CitizenConfiguration.CLIENT_ID;

import generated.se.sundsvall.citizen.CitizenExtended;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import se.sundsvall.paratransit.integration.citizen.configuration.CitizenConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.citizen.url}", configuration = CitizenConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface CitizenClient {

	/**
	 * Method for retrieving a citizen.
	 *
	 * @param  municipalityId                       the municipality ID.
	 * @param  personId                             the person ID.
	 * @return                                      An object with citizen data.
	 * @throws org.zalando.problem.ThrowableProblem when called service responds with error code.
	 */
	@GetMapping(path = "/{municipalityId}/{personId}", produces = APPLICATION_JSON_VALUE)
	Optional<CitizenExtended> getCitizen(
		@PathVariable("municipalityId") String municipalityId,
		@PathVariable("personId") String personId);
}

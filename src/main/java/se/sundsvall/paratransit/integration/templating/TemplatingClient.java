package se.sundsvall.paratransit.integration.templating;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.paratransit.integration.templating.configuration.TemplatingConfiguration.CLIENT_ID;

import generated.se.sundsvall.templating.RenderRequest;
import generated.se.sundsvall.templating.RenderResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import se.sundsvall.paratransit.integration.templating.configuration.TemplatingConfiguration;

@FeignClient(name = CLIENT_ID, url = "${integration.templating.url}", configuration = TemplatingConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface TemplatingClient {

	/**
	 * Render a stored template (with optional parameters) as a PDF
	 *
	 * @param  municipalityId the id of the municipality
	 * @param  pdfRequest     containing information regarding what template and version to use
	 * @return                a RenderResponse containing the rendered PDF
	 */
	@PostMapping(path = "/{municipalityId}/render/pdf", consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
	RenderResponse renderPdf(@PathVariable String municipalityId, @RequestBody RenderRequest pdfRequest);
}

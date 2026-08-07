package se.sundsvall.paratransit.integration.casedata;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Note;
import generated.se.sundsvall.casedata.PatchErrand;
import generated.se.sundsvall.casedata.Status;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import se.sundsvall.paratransit.integration.casedata.configuration.CaseDataConfiguration;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static se.sundsvall.paratransit.integration.casedata.configuration.CaseDataConfiguration.CLIENT_ID;

@FeignClient(name = CLIENT_ID, url = "${integration.casedata.url}", configuration = CaseDataConfiguration.class)
@CircuitBreaker(name = CLIENT_ID)
public interface CaseDataClient {

	/**
	 * Gets an errand by id.
	 *
	 * @param  errandId                                     of errand to get
	 * @throws se.sundsvall.dept44.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}", produces = APPLICATION_JSON_VALUE)
	Errand getErrandById(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId);

	/**
	 * Updates an errand.
	 *
	 * @param  patchErrand                                  for patching errand
	 * @param  errandId                                     of errand to update
	 * @throws se.sundsvall.dept44.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> patchErrand(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestBody PatchErrand patchErrand);

	@PatchMapping("/{municipalityId}/{namespace}/errands/{errandId}/status")
	ResponseEntity<Void> patchStatus(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestBody Status status);

	/**
	 * Gets notes by errand id.
	 *
	 * @param  errandId                                     of errand containing notes to get
	 * @throws se.sundsvall.dept44.problem.ThrowableProblem on error
	 */
	@GetMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/notes", produces = APPLICATION_JSON_VALUE)
	List<Note> getNotesByErrandId(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestParam(required = false) String noteType);

	/**
	 * Delete note by note id.
	 *
	 * @param  noteId                                       of note to delete
	 * @throws se.sundsvall.dept44.problem.ThrowableProblem on error
	 */
	@DeleteMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/notes/{noteId}")
	ResponseEntity<Void> deleteNoteById(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@PathVariable Long noteId);

	/**
	 * Create and add note.
	 *
	 * @param  extraParameters                              list of extra parameters to add or update(if existing)
	 * @throws se.sundsvall.dept44.problem.ThrowableProblem on error
	 */
	@PatchMapping(path = "/{municipalityId}/{namespace}/errands/{errandId}/extraparameters", consumes = APPLICATION_JSON_VALUE)
	ResponseEntity<Void> updateExtraParameters(
		@PathVariable String municipalityId,
		@PathVariable String namespace,
		@PathVariable Long errandId,
		@RequestBody List<ExtraParameter> extraParameters);
}

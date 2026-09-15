package se.sundsvall.paratransit.businesslogic.worker.followup;

import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.businesslogic.worker.AbstractWorker;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;
import se.sundsvall.paratransit.integration.engine.EngineClient;

import static generated.se.sundsvall.casedata.NoteType.INTERNAL;
import static java.util.Collections.emptyList;

@Component
@ExternalTaskSubscription("CleanUpNotesTask")
public class CleanUpNotesTaskWorker extends AbstractWorker {

	CleanUpNotesTaskWorker(final EngineClient engineClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler) {

		super(engineClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CleanUpNotesTask");

			final Long caseNumber = getCaseNumber(externalTask);
			final String namespace = getNamespace(externalTask);
			final String municipalityId = getMunicipalityId(externalTask);

			final var notes = caseDataClient.getNotesByErrandId(municipalityId, namespace, caseNumber, INTERNAL.getValue());

			Optional.ofNullable(notes).orElse(emptyList()).forEach(internalNote -> caseDataClient.deleteNoteById(municipalityId, namespace, caseNumber, internalNote.getId()));

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}

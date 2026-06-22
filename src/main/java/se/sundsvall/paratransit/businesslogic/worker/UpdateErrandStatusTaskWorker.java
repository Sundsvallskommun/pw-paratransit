package se.sundsvall.paratransit.businesslogic.worker;

import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;
import se.sundsvall.paratransit.integration.engine.EngineClient;

import static se.sundsvall.paratransit.integration.casedata.mapper.CaseDataMapper.toStatus;

@Component
@ExternalTaskSubscription("UpdateErrandStatusTask")
public class UpdateErrandStatusTaskWorker extends AbstractWorker {

	UpdateErrandStatusTaskWorker(final EngineClient engineClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler) {

		super(engineClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing update of status for errand with id {}", errand.getId());

			final var status = externalTask.getVariable("status").toString();
			final var statusDescription = Optional.ofNullable(externalTask.getVariable("statusDescription")).map(Object::toString).orElse(status);
			caseDataClient.patchStatus(municipalityId, namespace, errand.getId(), toStatus(status, statusDescription));

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}

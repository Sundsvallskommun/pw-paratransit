package se.sundsvall.paratransit.businesslogic.worker;

import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;

import java.util.Map;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;
import se.sundsvall.paratransit.service.MessagingService;

@Component
@ExternalTaskSubscription("SendSimplifiedServiceTask")
public class SendSimplifiedServiceTaskWorker extends AbstractWorker {

	private final MessagingService messagingService;

	SendSimplifiedServiceTaskWorker(final CamundaClient camundaClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler,
		final MessagingService messagingService) {
		super(camundaClient, caseDataClient, failureHandler);
		this.messagingService = messagingService;
	}

	@Override
	public void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing delivery of simplified service message to applicant for errand with id {}", errand.getId());

			final var messageId = messagingService.sendMessageSimplifiedService(municipalityId, errand).toString();

			externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_MESSAGE_ID, messageId));
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}

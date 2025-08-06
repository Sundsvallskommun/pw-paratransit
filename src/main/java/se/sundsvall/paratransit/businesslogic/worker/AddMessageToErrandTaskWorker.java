package se.sundsvall.paratransit.businesslogic.worker;

import static generated.se.sundsvall.casedata.MessageRequest.DirectionEnum.OUTBOUND;
import static java.util.Optional.ofNullable;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;
import static se.sundsvall.paratransit.integration.casedata.mapper.CaseDataMapper.toMessageAttachment;
import static se.sundsvall.paratransit.integration.casedata.mapper.CaseDataMapper.toMessageRequest;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;
import se.sundsvall.paratransit.service.MessagingService;
import se.sundsvall.paratransit.util.TextProvider;

@Component
@ExternalTaskSubscription("AddMessageToErrandTask")
public class AddMessageToErrandTaskWorker extends AbstractWorker {

	private final MessagingService messagingService;
	private final TextProvider textProvider;

	AddMessageToErrandTaskWorker(final CamundaClient camundaClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler,
		final MessagingService messagingService, final TextProvider textProvider) {
		super(camundaClient, caseDataClient, failureHandler);
		this.messagingService = messagingService;
		this.textProvider = textProvider;
	}

	@Override
	public void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Executing addition of decision message to errand with id {}", errand.getId());

			final var pdf = messagingService.renderPdfDecision(municipalityId, errand);
			final var attachment = toMessageAttachment(textProvider.getDenialTexts(municipalityId).getFilename(), APPLICATION_PDF_VALUE, pdf);
			final var messageId = ofNullable(externalTask.getVariable(CAMUNDA_VARIABLE_MESSAGE_ID))
				.map(String::valueOf)
				.orElseThrow(() -> Problem.valueOf(Status.INTERNAL_SERVER_ERROR, "Id of sent message could not be retreived from stored process variables"));

			caseDataClient.addMessage(municipalityId, namespace, caseNumber, toMessageRequest(messageId, textProvider.getDenialTexts(municipalityId).getSubject(), textProvider.getDenialTexts(municipalityId).getPlainBody(), errand, OUTBOUND,
				"ProcessEngine", attachment));

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}

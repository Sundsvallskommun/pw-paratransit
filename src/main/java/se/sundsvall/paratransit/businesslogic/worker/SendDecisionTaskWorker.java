package se.sundsvall.paratransit.businesslogic.worker;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static java.util.Collections.emptyList;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;

import generated.se.sundsvall.casedata.Decision;
import java.util.Map;
import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;
import se.sundsvall.paratransit.service.MessagingService;
import se.sundsvall.paratransit.util.TextProvider;

@Component
@ExternalTaskSubscription("SendDecisionTask")
public class SendDecisionTaskWorker extends AbstractWorker {

	private final MessagingService messagingService;
	private final TextProvider textProvider;

	SendDecisionTaskWorker(final CamundaClient camundaClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler,
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
			logInfo("Executing delivery of decision message to applicant for errand with id {}", errand.getId());

			final var isApproved = Optional.ofNullable(errand.getDecisions()).orElse(emptyList()).stream()
				.anyMatch(decision -> isApproved(decision.getDecisionOutcome()));

			// TODO: Customize texts for denial(non citizen and other) and approval
			final var templateId = isApproved ? textProvider.getApprovalTexts(municipalityId).getTemplateId() : textProvider.getDenialTexts(municipalityId).getTemplateId();

			final var pdf = messagingService.renderPdfDecision(municipalityId, errand, templateId);
			final var messageId = messagingService.sendDecisionMessage(municipalityId, errand, pdf, isApproved).toString();

			externalTaskService.complete(externalTask, Map.of(CAMUNDA_VARIABLE_MESSAGE_ID, messageId));
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isApproved(final Decision.DecisionOutcomeEnum decisionOutcome) {
		return APPROVAL.equals(decisionOutcome);
	}
}

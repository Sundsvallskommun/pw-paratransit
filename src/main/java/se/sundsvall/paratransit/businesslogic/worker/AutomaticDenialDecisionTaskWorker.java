package se.sundsvall.paratransit.businesslogic.worker;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.DISMISSAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpHeaders.LOCATION;
import static org.springframework.http.MediaType.APPLICATION_PDF_VALUE;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE;
import static se.sundsvall.paratransit.Constants.CATEGORY_BESLUT;
import static se.sundsvall.paratransit.Constants.ROLE_REPORTER;
import static se.sundsvall.paratransit.integration.casedata.mapper.CaseDataMapper.toAttachment;
import static se.sundsvall.paratransit.integration.casedata.mapper.CaseDataMapper.toDecision;
import static se.sundsvall.paratransit.integration.casedata.mapper.CaseDataMapper.toLaw;
import static se.sundsvall.paratransit.integration.casedata.mapper.CaseDataMapper.toStakeholder;
import static se.sundsvall.paratransit.util.TimerUtil.getControlMessageTime;

import generated.se.sundsvall.casedata.Stakeholder;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Objects;
import org.apache.commons.lang3.math.NumberUtils;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;
import se.sundsvall.paratransit.service.MessagingService;
import se.sundsvall.paratransit.util.TextProvider;

@Component
@ExternalTaskSubscription("AutomaticDenialDecisionTask")
public class AutomaticDenialDecisionTaskWorker extends AbstractWorker {

	private static final String PROCESS_ENGINE_FIRST_NAME = "Process";
	private static final String PROCESS_ENGINE_LAST_NAME = "Engine";

	private final MessagingService messagingService;
	private final TextProvider textProvider;

	AutomaticDenialDecisionTaskWorker(final CamundaClient camundaClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler,
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
			logInfo("Executing automatic addition of dismissal to errand with id {}", errand.getId());

			// PE needs to be added as stakeholder to the errand (if not already present) and store for later use when setting
			// "decidedBy" on decision
			final var stakeholder = ofNullable(errand.getStakeholders()).orElse(emptyList())
				.stream()
				.filter(AutomaticDenialDecisionTaskWorker::isProcessEngineStakeholder)
				.findAny()
				.orElseGet(() -> createProcessEngineStakeholder(errand.getId(), municipalityId, namespace));

			final var pdf = messagingService.renderPdfDecision(municipalityId, errand, textProvider.getDenialTexts(municipalityId).getTemplateId());
			final var decision = toDecision(FINAL, DISMISSAL, textProvider.getDenialTexts(municipalityId).getDescription())
				.decidedBy(stakeholder)
				.decidedAt(OffsetDateTime.now())
				.addLawItem(toLaw(textProvider.getDenialTexts(municipalityId).getLawHeading(), textProvider.getDenialTexts(municipalityId).getLawSfs(),
					textProvider.getDenialTexts(municipalityId).getLawChapter(), textProvider.getDenialTexts(municipalityId).getLawArticle()))
				.addAttachmentsItem(toAttachment(CATEGORY_BESLUT, textProvider.getDenialTexts(municipalityId).getFilename(), "pdf", APPLICATION_PDF_VALUE, pdf));

			caseDataClient.patchNewDecision(municipalityId, namespace, errand.getId(), decision);

			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE, getControlMessageTime(decision, textProvider.getSimplifiedServiceTexts(municipalityId).getDelay()));

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private Stakeholder createProcessEngineStakeholder(final Long errandId, final String municipalityId, final String namespace) {
		final var id = extractStakeholderId(caseDataClient.addStakeholderToErrand(municipalityId, namespace, errandId, toStakeholder(ROLE_REPORTER, PERSON, PROCESS_ENGINE_FIRST_NAME, PROCESS_ENGINE_LAST_NAME)));
		return caseDataClient.getStakeholder(municipalityId, namespace, errandId, id);
	}

	private Long extractStakeholderId(final ResponseEntity<Void> response) {
		return ofNullable(response.getHeaders().get(LOCATION)).orElse(emptyList()).stream()
			.filter(Objects::nonNull)
			.map(locationValue -> locationValue.substring(locationValue.lastIndexOf('/') + 1))
			.filter(NumberUtils::isCreatable)
			.map(Long::valueOf)
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(Status.BAD_GATEWAY, "CaseData integration did not return any location for created stakeholder"));
	}

	private static boolean isProcessEngineStakeholder(final Stakeholder stakeholder) {
		// TODO: Check if this role should be used
		return stakeholder.getRoles().contains(ROLE_REPORTER) &&
			PROCESS_ENGINE_FIRST_NAME.equals(stakeholder.getFirstName()) &&
			PROCESS_ENGINE_LAST_NAME.equals(stakeholder.getLastName());
	}
}

package se.sundsvall.paratransit.businesslogic.worker.decision;

import static generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum.APPROVAL;
import static generated.se.sundsvall.casedata.Decision.DecisionTypeEnum.FINAL;
import static java.util.Collections.emptyList;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_FINAL_DECISION;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_IS_APPROVED;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_PHASE_STATUS;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE;
import static se.sundsvall.paratransit.Constants.CASEDATA_PHASE_DECISION;
import static se.sundsvall.paratransit.Constants.CASEDATA_STATUS_CASE_DECIDED;
import static se.sundsvall.paratransit.Constants.CASEDATA_STATUS_DECISION_EXECUTED;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_WAITING;
import static se.sundsvall.paratransit.integration.casedata.mapper.CaseDataMapper.toPatchErrand;
import static se.sundsvall.paratransit.util.TimerUtil.getControlMessageTime;

import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Errand;
import java.util.HashMap;
import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.businesslogic.worker.AbstractWorker;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;
import se.sundsvall.paratransit.util.TextProvider;

@Component
@ExternalTaskSubscription("CheckDecisionTask")
public class CheckDecisionTaskWorker extends AbstractWorker {

	private final TextProvider textProvider;

	CheckDecisionTaskWorker(final CamundaClient camundaClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler, final TextProvider textProvider) {
		super(camundaClient, caseDataClient, failureHandler);
		this.textProvider = textProvider;

	}

	@Override
	public void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute Worker for CheckDecisionTask");
			clearUpdateAvailable(externalTask);
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);

			final var variables = new HashMap<String, Object>();

			Optional.ofNullable(errand.getStatuses()).orElse(emptyList()).stream()
				.filter(status -> CASEDATA_STATUS_CASE_DECIDED.equals(status.getStatusType()) || CASEDATA_STATUS_DECISION_EXECUTED.equals(status.getStatusType()))
				.findFirst()
				.ifPresentOrElse(status -> {
					if (isFinalDecision(errand)) {
						variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, true);
						logInfo("Decision is made.");
						variables.put(CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE, getControlMessageTime(getFinalDecision(errand), textProvider.getSimplifiedServiceTexts(municipalityId).getDelay()));
					} else {
						variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, false);
						variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING);
						caseDataClient.patchErrand(municipalityId, errand.getNamespace(), errand.getId(), toPatchErrand(errand, CASEDATA_PHASE_DECISION, CASEDATA_PHASE_DECISION, PHASE_STATUS_WAITING, PHASE_ACTION_UNKNOWN));
						logInfo("Decision is not made yet.");
					}
				}, () -> {
					variables.put(CAMUNDA_VARIABLE_FINAL_DECISION, false);
					variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING);
					caseDataClient.patchErrand(municipalityId, errand.getNamespace(), errand.getId(), toPatchErrand(errand, CASEDATA_PHASE_DECISION, CASEDATA_PHASE_DECISION, PHASE_STATUS_WAITING, PHASE_ACTION_UNKNOWN));
					logInfo("Decision is not made yet.");
				});

			Optional.ofNullable(errand.getDecisions()).orElse(emptyList()).stream()
				.filter(decision -> isApproved(decision.getDecisionOutcome()))
				.findFirst()
				.ifPresentOrElse(decision -> variables.put(CAMUNDA_VARIABLE_IS_APPROVED, true),
					() -> variables.put(CAMUNDA_VARIABLE_IS_APPROVED, false));

			if (isCancel(errand)) {
				variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_CANCEL);
				variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_CANCELED);
			}

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isApproved(final Decision.DecisionOutcomeEnum decisionOutcome) {
		return APPROVAL.equals(decisionOutcome);
	}

	private boolean isFinalDecision(final Errand errand) {
		if (errand.getDecisions() == null) {
			return false;
		}
		return errand.getDecisions().stream()
			.anyMatch(decision -> FINAL.equals(decision.getDecisionType()));
	}

	private Decision getFinalDecision(final Errand errand) {
		return errand.getDecisions().stream()
			.filter(decision -> FINAL.equals(decision.getDecisionType()))
			.findFirst()
			.orElse(null);
	}
}

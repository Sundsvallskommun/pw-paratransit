package se.sundsvall.paratransit.businesslogic.worker.actualization;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_HAS_ADMINISTRATOR_AND_APPLICANT;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_PHASE_STATUS;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_WAITING;
import static se.sundsvall.paratransit.Constants.ROLE_ADMINISTRATOR;
import static se.sundsvall.paratransit.Constants.ROLE_APPLICANT;
import static se.sundsvall.paratransit.integration.casedata.mapper.CaseDataMapper.toExtraParameters;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Stakeholder;
import java.util.HashMap;
import java.util.Objects;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.businesslogic.worker.AbstractWorker;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("VerifyStakeholdersExists")
public class VerifyStakeholdersExistsTaskWorker extends AbstractWorker {

	VerifyStakeholdersExistsTaskWorker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute task for evaluating if stakeholders with role 'ADMINISTRATOR' and 'APPLICANT' is present.");
			clearUpdateAvailable(externalTask);

			final var municipalityId = getMunicipalityId(externalTask);
			final var namespace = getNamespace(externalTask);
			final var caseNumber = getCaseNumber(externalTask);
			final var errand = getErrand(municipalityId, namespace, caseNumber);

			final var hasAdministratorAndApplicant = hasAdministratorAndApplicant(errand);
			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_HAS_ADMINISTRATOR_AND_APPLICANT, hasAdministratorAndApplicant);

			if (isCancel(errand)) {
				logInfo("Cancel has been requested for errand with id {}", errand.getId());

				caseDataClient.updateExtraParameters(municipalityId, namespace, errand.getId(), toExtraParameters(PHASE_STATUS_CANCELED, PHASE_ACTION_CANCEL));
				variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_CANCEL);
				variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_CANCELED);

			} else if (!hasAdministratorAndApplicant) {
				caseDataClient.updateExtraParameters(municipalityId, namespace, errand.getId(), toExtraParameters(PHASE_STATUS_WAITING, PHASE_ACTION_UNKNOWN));
				variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING);
			}

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean hasAdministratorAndApplicant(Errand errand) {
		final var hasAdministrator = ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.map(Stakeholder::getRoles)
			.filter(Objects::nonNull)
			.anyMatch(roles -> roles.contains(ROLE_ADMINISTRATOR) && !roles.contains(ROLE_APPLICANT));
		final var hasApplicant = ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.map(Stakeholder::getRoles)
			.filter(Objects::nonNull)
			.anyMatch(roles -> roles.contains(ROLE_APPLICANT) && !roles.contains(ROLE_ADMINISTRATOR));
		return hasAdministrator && hasApplicant;
	}
}

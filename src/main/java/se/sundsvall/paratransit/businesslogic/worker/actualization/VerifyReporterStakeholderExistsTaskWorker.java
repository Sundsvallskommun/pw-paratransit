package se.sundsvall.paratransit.businesslogic.worker.actualization;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_ASSIGNED_TO_REPORTER;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_PHASE_STATUS;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_WAITING;
import static se.sundsvall.paratransit.Constants.ROLE_REPORTER;
import static se.sundsvall.paratransit.integration.casedata.mapper.CaseDataMapper.toPatchErrand;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Stakeholder;
import java.util.HashMap;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.businesslogic.worker.AbstractWorker;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("VerifyReporterStakeholderExists")
public class VerifyReporterStakeholderExistsTaskWorker extends AbstractWorker {

	VerifyReporterStakeholderExistsTaskWorker(final CamundaClient camundaClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler) {

		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	protected void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute task for evaluating if stakeholder with role 'ADMINISTRATOR' is present.");
			clearUpdateAvailable(externalTask);

			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);
			final var errand = getErrand(municipalityId, namespace, caseNumber);

			final var reporterIsAssigned = isReporterAssigned(errand);
			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_ASSIGNED_TO_REPORTER, reporterIsAssigned);

			if (isCancel(errand)) {
				logInfo("Cancel has been requested for errand with id {}", errand.getId());

				caseDataClient.patchErrand(municipalityId, namespace, errand.getId(), toPatchErrand(errand, errand.getPhase(), PHASE_STATUS_CANCELED, PHASE_ACTION_CANCEL));
				variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_CANCEL);
				variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_CANCELED);

			} else if (!reporterIsAssigned) {
				caseDataClient.patchErrand(municipalityId, namespace, errand.getId(), toPatchErrand(errand, errand.getPhase(), PHASE_STATUS_WAITING, PHASE_ACTION_UNKNOWN));
				variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING);
			}

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isReporterAssigned(final Errand errand) {
		final var isControlOfficialAssigned = ofNullable(errand.getStakeholders()).orElse(emptyList()).stream()
			.map(Stakeholder::getRoles)
			.anyMatch(roles -> roles.contains(ROLE_REPORTER));

		logInfo("Errand with id {} {} been assigned to a control official", errand.getId(), isControlOfficialAssigned ? "has" : "has not yet");
		return isControlOfficialAssigned;
	}
}

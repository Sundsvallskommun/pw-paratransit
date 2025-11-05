package se.sundsvall.paratransit.businesslogic.worker.actualization;

import static java.util.Objects.isNull;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_IS_DRAFT;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_PHASE_STATUS;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_STATUS;
import static se.sundsvall.paratransit.Constants.CASEDATA_STATUS_DRAFT;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_WAITING;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import java.util.HashMap;
import java.util.List;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.businesslogic.worker.AbstractWorker;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("VerifyStatus")
public class VerifyStatusTaskWorker extends AbstractWorker {

	VerifyStatusTaskWorker(final CamundaClient camundaClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler) {

		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	protected void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			logInfo("Execute task for evaluating status of errand and wait if status is Utkast");
			clearUpdateAvailable(externalTask);

			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);
			final var errand = getErrand(municipalityId, namespace, caseNumber);
			final var variables = new HashMap<String, Object>();
			final var isDraft = isDraft(errand);
			variables.put(CAMUNDA_VARIABLE_IS_DRAFT, isDraft);

			if (isCancel(errand)) {
				logInfo("Cancel has been requested for errand with id {}", errand.getId());
				caseDataClient.updateExtraParameters(municipalityId, namespace, errand.getId(), List.of(
					new ExtraParameter().key(CASEDATA_KEY_PHASE_STATUS).values(List.of(PHASE_STATUS_CANCELED)),
					new ExtraParameter().key(CASEDATA_KEY_PHASE_ACTION).values(List.of(PHASE_ACTION_CANCEL))));
				variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_CANCEL);
				variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_CANCELED);

			} else if (isDraft) {
				caseDataClient.updateExtraParameters(municipalityId, namespace, errand.getId(), List.of(new ExtraParameter().key(CASEDATA_KEY_PHASE_STATUS).values(List.of(PHASE_STATUS_WAITING)),
					new ExtraParameter().key(CASEDATA_KEY_PHASE_ACTION).values(List.of(PHASE_ACTION_UNKNOWN))));
				variables.put(CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING);
			}

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isDraft(final Errand errand) {
		if (isNull(errand.getStatus())) {
			return false;
		}
		return CASEDATA_STATUS_DRAFT.equals(errand.getStatus().getStatusType());
	}
}

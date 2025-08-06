package se.sundsvall.paratransit.businesslogic.worker;

import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_IS_APPEAL;
import static se.sundsvall.paratransit.Constants.CASE_TYPE_APPEAL;

import generated.se.sundsvall.casedata.Errand;
import java.util.HashMap;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("CheckAppealTask")
public class CheckAppealTaskWorker extends AbstractWorker {

	CheckAppealTaskWorker(final CamundaClient camundaClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler) {

		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Check if errand is an appeal for errand with id {}", errand.getId());

			final var isAppeal = isAppeal(errand);

			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_IS_APPEAL, isAppeal);

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isAppeal(final Errand errand) {
		return CASE_TYPE_APPEAL.equals(errand.getCaseType());
	}
}

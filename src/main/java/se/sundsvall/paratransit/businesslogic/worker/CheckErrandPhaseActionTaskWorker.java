package se.sundsvall.paratransit.businesslogic.worker;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_STATUS;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_COMPLETE;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_COMPLETED;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_WAITING;
import static se.sundsvall.paratransit.integration.casedata.mapper.CaseDataMapper.toExtraParameters;

import generated.se.sundsvall.casedata.Errand;
import java.util.HashMap;
import java.util.Optional;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;

@Component
@ExternalTaskSubscription("CheckErrandPhaseActionTask")
public class CheckErrandPhaseActionTaskWorker extends AbstractWorker {

	CheckErrandPhaseActionTaskWorker(final CamundaClient camundaClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler) {

		super(camundaClient, caseDataClient, failureHandler);
	}

	@Override
	public void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		try {
			clearUpdateAvailable(externalTask);
			final String municipalityId = getMunicipalityId(externalTask);
			final String namespace = getNamespace(externalTask);
			final Long caseNumber = getCaseNumber(externalTask);

			final var errand = getErrand(municipalityId, namespace, caseNumber);
			logInfo("Check phase action for errand with id {}", errand.getId());

			final var phaseAction = ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
				.filter(extraParameters -> CASEDATA_KEY_PHASE_ACTION.equals(extraParameters.getKey()))
				.findFirst()
				.flatMap(extraParameters -> extraParameters.getValues().stream().findFirst())
				.orElse(PHASE_ACTION_UNKNOWN);

			final var displayPhase = ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
				.filter(extraParameters -> CASEDATA_KEY_DISPLAY_PHASE.equals(extraParameters.getKey()))
				.findFirst()
				.flatMap(extraParameters -> extraParameters.getValues().stream().findFirst())
				.orElse(CASEDATA_KEY_DISPLAY_PHASE);

			switch (phaseAction) {
				case PHASE_ACTION_COMPLETE -> {
					logInfo("Phase action is complete. Setting phase status to {}", PHASE_STATUS_COMPLETED);
					caseDataClient.updateExtraParameters(municipalityId, namespace, errand.getId(), toExtraParameters(displayPhase, PHASE_STATUS_COMPLETED, phaseAction));
				}
				case PHASE_ACTION_CANCEL -> {
					logInfo("Phase action is cancel. Setting phase status to {}", PHASE_STATUS_CANCELED);
					caseDataClient.updateExtraParameters(municipalityId, namespace, errand.getId(), toExtraParameters(displayPhase, PHASE_STATUS_CANCELED, phaseAction));
				}
				default -> {
					logInfo("Phase action is unknown. Setting phase status to {}", PHASE_STATUS_WAITING);
					if (isPhaseStatusNotWaiting(errand)) {
						caseDataClient.updateExtraParameters(municipalityId, namespace, errand.getId(), toExtraParameters(displayPhase, PHASE_STATUS_WAITING, phaseAction));
					}
				}
			}

			final var variables = new HashMap<String, Object>();
			variables.put(CAMUNDA_VARIABLE_PHASE_ACTION, phaseAction);

			externalTaskService.complete(externalTask, variables);
		} catch (final Exception exception) {
			logException(externalTask, exception);
			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}

	private boolean isPhaseStatusNotWaiting(final Errand errand) {
		return !PHASE_STATUS_WAITING.equals(Optional.ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameters -> CASEDATA_KEY_PHASE_STATUS.equals(extraParameters.getKey()))
			.findFirst()
			.map(extraParameters -> extraParameters.getValues().getFirst())
			.orElse(null));
	}
}

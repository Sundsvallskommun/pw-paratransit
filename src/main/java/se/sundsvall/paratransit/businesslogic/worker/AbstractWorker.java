package se.sundsvall.paratransit.businesslogic.worker;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.FALSE;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.paratransit.Constants.UPDATE_AVAILABLE;

import generated.se.sundsvall.camunda.VariableValueDto;
import generated.se.sundsvall.casedata.Attachment;
import generated.se.sundsvall.casedata.Errand;
import java.util.List;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;

public abstract class AbstractWorker implements ExternalTaskHandler {

	private final Logger logger;

	private final CamundaClient camundaClient;

	protected final CaseDataClient caseDataClient;

	protected final FailureHandler failureHandler;

	protected AbstractWorker(final CamundaClient camundaClient, final CaseDataClient caseDataClient, final FailureHandler failureHandler) {
		this.logger = LoggerFactory.getLogger(getClass());
		this.camundaClient = camundaClient;
		this.caseDataClient = caseDataClient;
		this.failureHandler = failureHandler;
	}

	protected void clearUpdateAvailable(final ExternalTask externalTask) {
		/*
		 * Clearing process variable has to be a blocking operation.
		 * Using ExternalTaskService.setVariables() will not work without creating race conditions.
		 */
		camundaClient.setProcessInstanceVariable(externalTask.getProcessInstanceId(), UPDATE_AVAILABLE, FALSE);
	}

	protected void setProcessInstanceVariable(final ExternalTask externalTask, final String variableName, final VariableValueDto variableValue) {

		camundaClient.setProcessInstanceVariable(externalTask.getProcessInstanceId(), variableName, variableValue);
	}

	protected Errand getErrand(final String municipalityId, final String namespace, final Long caseNumber) {

		return caseDataClient.getErrandById(municipalityId, namespace, caseNumber);
	}

	protected List<Attachment> getErrandAttachments(final String municipalityId, final String namespace, final Long caseNumber) {
		return caseDataClient.getErrandAttachments(municipalityId, namespace, caseNumber);
	}

	protected void logInfo(final String msg, final Object... arguments) {
		logger.info(msg, arguments);
	}

	protected void logException(final ExternalTask externalTask, final Exception exception) {
		logger.error("Exception occurred in {} for task with id {} and businesskey {}", this.getClass().getSimpleName(), externalTask.getId(), externalTask.getBusinessKey(), exception);
	}

	protected abstract void executeBusinessLogic(final ExternalTask externalTask, final ExternalTaskService externalTaskService);

	@Override
	public void execute(final ExternalTask externalTask, final ExternalTaskService externalTaskService) {
		RequestId.init(externalTask.getVariable(CAMUNDA_VARIABLE_REQUEST_ID));
		executeBusinessLogic(externalTask, externalTaskService);
	}

	protected String getMunicipalityId(final ExternalTask externalTask) {

		return externalTask.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
	}

	protected String getNamespace(final ExternalTask externalTask) {

		return externalTask.getVariable(CAMUNDA_VARIABLE_NAMESPACE);
	}

	protected Long getCaseNumber(final ExternalTask externalTask) {

		return externalTask.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
	}

	protected boolean isCancel(final Errand errand) {
		return ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
			.filter(extraParameters -> CASEDATA_KEY_PHASE_ACTION.equals(extraParameters.getKey()))
			.findFirst()
			.map(extraParameters -> extraParameters.getValues().getFirst())
			.filter(PHASE_ACTION_CANCEL::equals)
			.isPresent();
	}
}

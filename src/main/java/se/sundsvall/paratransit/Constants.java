package se.sundsvall.paratransit;

import generated.se.sundsvall.camunda.VariableValueDto;
import org.camunda.bpm.engine.variable.type.ValueType;

public class Constants {

	private Constants() {}

	public static final String PROCESS_KEY = "process-paratransit"; // Must match ID of process defined in bpmn schema
	public static final String TENANTID_TEMPLATE = "PARATRANSIT"; // Namespace where process is deployed, a.k.a tenant (must match setting in application.yaml)

	public static final String NAMESPACE_REGEXP = "[\\w|\\-]+";
	public static final String NAMESPACE_VALIDATION_MESSAGE = "can only contain A-Z, a-z, 0-9, -, and _";

	public static final String UPDATE_AVAILABLE = "updateAvailable";
	public static final VariableValueDto TRUE = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(true);
	public static final VariableValueDto FALSE = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(false);

	public static final String CAMUNDA_VARIABLE_APPLICANT_NOT_RESIDENT_OF_MUNICIPALITY = "applicantNotResidentOfMunicipality";
	public static final String CAMUNDA_VARIABLE_ASSIGNED_TO_REPORTER = "assignedToReporter";
	public static final String CAMUNDA_VARIABLE_CASE_NUMBER = "caseNumber";
	public static final String CAMUNDA_VARIABLE_REQUEST_ID = "requestId";
	public static final String CAMUNDA_VARIABLE_MUNICIPALITY_ID = "municipalityId";
	public static final String CAMUNDA_VARIABLE_NAMESPACE = "namespace";
	public static final String CAMUNDA_VARIABLE_UPDATE_AVAILABLE = "updateAvailable";
	public static final String CAMUNDA_VARIABLE_MESSAGE_ID = "messageId";
	public static final String CAMUNDA_VARIABLE_FINAL_DECISION = "finalDecision";
	public static final String CAMUNDA_VARIABLE_IS_APPROVED = "isApproved";
	public static final String CAMUNDA_VARIABLE_PHASE = "phase";
	public static final String CAMUNDA_VARIABLE_PHASE_STATUS = "phaseStatus";
	public static final String CAMUNDA_VARIABLE_PHASE_ACTION = "phaseAction";
	public static final String CAMUNDA_VARIABLE_DISPLAY_PHASE = "displayPhase";
	public static final String CAMUNDA_VARIABLE_TIME_TO_SEND_CONTROL_MESSAGE = "timeToSendControlMessage";
	public static final String CAMUNDA_VARIABLE_IS_APPEAL = "isAppeal";

	public static final String CASEDATA_KEY_PHASE_STATUS = "process.phaseStatus";
	public static final String CASEDATA_KEY_PHASE_ACTION = "process.phaseAction";
	public static final String CASEDATA_KEY_DISPLAY_PHASE = "process.displayPhase";

	public static final String CASEDATA_PHASE_ACTUALIZATION = "Aktualisering";
	public static final String CASEDATA_PHASE_INVESTIGATION = "Utredning";
	public static final String CASEDATA_PHASE_DECISION = "Beslut";
	public static final String CASEDATA_PHASE_CANCELED = "Avslutad";
	public static final String CASEDATA_PHASE_EXECUTION = "Verkställa";
	public static final String CASEDATA_PHASE_FOLLOW_UP = "Uppföljning";

	public static final String PHASE_ACTION_COMPLETE = "COMPLETE";
	public static final String PHASE_ACTION_CANCEL = "CANCEL";
	public static final String PHASE_ACTION_UNKNOWN = "UNKNOWN";
	public static final String PHASE_STATUS_COMPLETED = "COMPLETED";
	public static final String PHASE_STATUS_ONGOING = "ONGOING";
	public static final String PHASE_STATUS_WAITING = "WAITING";
	public static final String PHASE_STATUS_CANCELED = "CANCELED";

	public static final String CASEDATA_STATUS_CASE_RECEIVED = "Ärende inkommit";
	public static final String CASEDATA_STATUS_AWAITING_COMPLETION = "Väntar på komplettering";
	public static final String CASEDATA_STATUS_COMPLETION_RECEIVED = "Komplettering inkommen";
	public static final String CASEDATA_STATUS_CASE_PROCESS = "Under utredning";
	public static final String CASEDATA_STATUS_CASE_DECIDE = "Under beslut";
	public static final String CASEDATA_STATUS_DECISION_EXECUTED = "Beslut verkställt";
	public static final String CASEDATA_STATUS_CASE_DECIDED = "Beslutad";
	public static final String CASEDATA_STATUS_CASE_FINALIZED = "Ärende avslutat";

	public static final String CATEGORY_BESLUT = "BESLUT";

	public static final String ROLE_APPLICANT = "APPLICANT";
	public static final String ROLE_REPORTER = "REPORTER";

	public static final String CASE_TYPE_APPEAL = "APPEAL";
	public static final String CASE_TYPE_PARATRANSIT = "PARATRANSIT";

	public static final String MESSAGING_KEY_FLOW_INSTANCE_ID = "flowInstanceId";

}

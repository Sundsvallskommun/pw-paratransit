package apptest.mock;

import static apptest.mock.api.CaseData.createPatchErrandBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_COMPLETE;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;

import java.util.Map;

public class Investigation {

	public static String mockInvestigation(String caseId, String scenarioName) {
		var scenarioAfterUpdatePhase = mockInvestigationUpdatePhase(caseId, scenarioName, "actualization_check-phase-action_task-worker---api-casedata-patch-errand");
		var scenarioAfterUpdateStatus = mockInvestigationUpdateStatus(caseId, scenarioName, scenarioAfterUpdatePhase);
		return mockInvestigationCheckPhaseAction(caseId, scenarioName, scenarioAfterUpdateStatus);
	}

	public static String mockInvestigationUpdatePhase(String caseId, String scenarioName, String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"investigation_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "COMPLETE",
				"phaseActionParameter", PHASE_ACTION_COMPLETE,
				"displayPhaseParameter", "Aktualisering"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"investigation_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchErrandBody("Utredning")));
			//equalToJson(createPatchBody("Utredning", PHASE_ACTION_UNKNOWN, "ONGOING", "Utredning")));
	}

	public static String mockInvestigationUpdateStatus(String caseId, String scenarioName, String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"investigation_update-status-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"statusTypeParameter", "Ärende inkommit",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Utredning"));

		return mockCaseDataPatchStatus(caseId, scenarioName, state,
			"investigation_update-status-task-worker---api-casedata-patch-status",
			equalToJson("""
				  {
				    "statusType": "Under utredning",
				    "description": "Ärendet utreds",
				    "created": "${json-unit.any-string}"
				  }
				"""));
	}

	public static String mockInvestigationCheckPhaseAction(String caseId, String scenarioName, String requiredScenarioState) {
		return mockInvestigationCheckPhaseAction(caseId, scenarioName, requiredScenarioState, null);
	}

	public static String mockInvestigationCheckPhaseAction(String caseId, String scenarioName, String requiredScenarioState, String newScenarioStateSuffix) {
		var newScenarioStateGet = "investigation_check-phase-action_task-worker---api-casedata-get-errand";
		if (newScenarioStateSuffix != null) {
			newScenarioStateGet = newScenarioStateGet.concat(newScenarioStateSuffix);
		}
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioStateGet,
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_COMPLETE,
				"displayPhaseParameter", "Utredning"));

		var newScenarioStatePatch = "investigation_check-phase-action_task-worker---api-casedata-patch-errand";
		if (newScenarioStateSuffix != null) {
			newScenarioStatePatch = newScenarioStatePatch.concat(newScenarioStateSuffix);
		}
		return mockCaseDataPatch(caseId, scenarioName, state, newScenarioStatePatch,
			equalToJson(createPatchErrandBody("Utredning")));
			//equalToJson(createPatchBody("Utredning", PHASE_ACTION_COMPLETE, "COMPLETED", "Utredning")));
	}
}

package apptest.mock;

import static apptest.mock.api.CaseData.createPatchErrandBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchExtraParameters;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;

import java.util.Map;

public class Decision {

	public static String mockDecision(final String caseId, final String scenarioName) {
		var scenarioAfterUpdatePhase = mockDecisionUpdatePhase(caseId, scenarioName, "investigation_check-phase-action_task-worker---api-casedata-patch-extra-parameters");
		var scenarioAfterUpdateStatus = mockDecisionUpdateStatus(caseId, scenarioName, scenarioAfterUpdatePhase);
		return mockDecisionCheckIfDecisionMade(caseId, scenarioName, scenarioAfterUpdateStatus);
	}

	public static String mockDecisionUpdatePhase(final String caseId, final String scenarioName, final String requiredScenarioState) {

		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"decision_update-phase-task-worker---api-casedata-patch-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Utredning",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN,
				"phaseStatusParameter", "ONGOING",
				"displayPhaseParameter", "Utredning"));

		final var stateAfterErrandPatch = mockCaseDataPatch(caseId, scenarioName, state,
			"decision_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchErrandBody("Beslut")));
		return mockCaseDataPatchExtraParameters(caseId, scenarioName, stateAfterErrandPatch,
			"decision_update-phase-task-worker---api-casedata-patch-extra-parameters",
			equalToJson("""
				 [
				    {
				        "key":"process.phaseStatus",
				        "values":["ONGOING"]
				    },
				    {
				        "key":"process.phaseAction",
				        "values":["UNKNOWN"]
					},
					{
				        "key":"process.displayPhase",
				        "values":["Beslut"]
				    }
				]
				"""));
	}

	public static String mockDecisionUpdateStatus(final String caseId, final String scenarioName, final String requiredScenarioState) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"decision_update-status-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"statusTypeParameter", "Ärende inkommit",
				"phaseParameter", "Beslut",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Beslut"));

		return mockCaseDataPatchStatus(caseId, scenarioName, state,
			"decision_update-status-task-worker---api-casedata-patch-status",
			equalToJson("""
				  {
				    "statusType": "Under beslut",
				    "description": "Ärendet beslutas",
				    "created": "${json-unit.any-string}"
				  }
				"""));
	}

	public static String mockDecisionCheckIfDecisionMade(final String caseId, final String scenarioName, final String requiredScenarioState) {
		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"check-decision-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"displayPhaseParameter", "Beslut",
				"statusTypeParameter", "Beslutad"));
	}
}

package apptest.mock;

import static apptest.mock.api.CaseData.createPatchErrandBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchExtraParameters;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_COMPLETE;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;

import java.util.Map;

public class Investigation {

	public static String mockInvestigation(final String caseId, final String scenarioName) {
		var scenarioAfterUpdatePhase = mockInvestigationUpdatePhase(caseId, scenarioName, "actualization_check-phase-action-task-worker---api-casedata-patch-extra-parameters");
		var scenarioAfterUpdateStatus = mockInvestigationUpdateStatus(caseId, scenarioName, scenarioAfterUpdatePhase);
		return mockInvestigationCheckPhaseAction(caseId, scenarioName, scenarioAfterUpdateStatus);
	}

	public static String mockInvestigationUpdatePhase(final String caseId, final String scenarioName, final String requiredScenarioState) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"investigation_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "COMPLETED",
				"phaseActionParameter", PHASE_ACTION_COMPLETE,
				"displayPhaseParameter", "Granskning"));

		final var stateAfterErrandPatch = mockCaseDataPatch(caseId, scenarioName, state,
			"investigation_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchErrandBody("Utredning")));

		return mockCaseDataPatchExtraParameters(caseId, scenarioName, stateAfterErrandPatch,
			"investigation_update-phase-task-worker---api-casedata-patch-extra-parameters",
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
				        "values":["Utredning"]
				    }
				]
				"""));
	}

	public static String mockInvestigationUpdateStatus(final String caseId, final String scenarioName, final String requiredScenarioState) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
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

	public static String mockInvestigationCheckPhaseAction(final String caseId, final String scenarioName, final String requiredScenarioState) {

		return mockInvestigationCheckPhaseAction(caseId, scenarioName, requiredScenarioState, null);
	}

	public static String mockInvestigationCheckPhaseAction(final String caseId, final String scenarioName, final String requiredScenarioState, final String newScenarioStateSuffix) {
		var newScenarioStateGet = "investigation_check-phase-action_task-worker---api-casedata-get-errand";
		if (newScenarioStateSuffix != null) {
			newScenarioStateGet = newScenarioStateGet.concat(newScenarioStateSuffix);
		}
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState, newScenarioStateGet,
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Utredning",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_COMPLETE,
				"displayPhaseParameter", "Utredning"));

		var newScenarioStatePatch = "investigation_check-phase-action_task-worker---api-casedata-patch-extra-parameters";
		if (newScenarioStateSuffix != null) {
			newScenarioStatePatch = newScenarioStatePatch.concat(newScenarioStateSuffix);
		}

		return mockCaseDataPatchExtraParameters(caseId, scenarioName, state, newScenarioStatePatch,
			equalToJson("""
				 [
				    {
				        "key":"process.phaseStatus",
				        "values":["COMPLETED"]
				    },
				    {
				        "key":"process.phaseAction",
				        "values":["COMPLETE"]
					},
					{
				        "key":"process.displayPhase",
				        "values":["Utredning"]
				    }
				]
				"""));
	}
}

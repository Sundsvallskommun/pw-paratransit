package apptest.mock;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_COMPLETE;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;

import java.util.Map;

public class Actualization {

	public static String mockActualization(String caseId, String scenarioName) {
		var scenarioAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, "check_appeal_check-appeal-task-worker---api-casedata-get-errand");
		var scenarioAfterVerifyReporter = mockActualizationVerifyReporterStakeholder(caseId, scenarioName, scenarioAfterUpdatePhase);
		var scenarioAfterUpdateDisplayPhase = mockActualizationUpdateDisplayPhase(caseId, scenarioName, scenarioAfterVerifyReporter);
		var scenarioAfterUpdateStatus = mockActualizationUpdateStatus(caseId, scenarioName, scenarioAfterUpdateDisplayPhase);
		return mockActualizationCheckPhaseAction(caseId, scenarioName, scenarioAfterUpdateStatus);
	}

	public static String mockActualizationUpdatePhase(String caseId, String scenarioName, String requiredScenarioState) {

		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"displayPhaseParameter", "Aktualisering",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"actualization_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Aktualisering", PHASE_ACTION_UNKNOWN, "ONGOING", "Registrerad")));
	}

	public static String mockActualizationVerifyReporterStakeholder(String caseId, String scenarioName, String requiredScenarioState) {
		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_verify-administrator-stakeholder---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Registrerad"));
	}

	public static String mockActualizationUpdateDisplayPhase(String caseId, String scenarioName, String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_update-display-phase---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Registrerad"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"actualization_update-display-phase---api-casedata-patch-errand",
			equalToJson(createPatchBody("Aktualisering", PHASE_ACTION_UNKNOWN, "ONGOING", "Granskning")));
	}

	public static String mockActualizationUpdateStatus(String caseId, String scenarioName, String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_update-errand-status---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Granskning"));

		return mockCaseDataPatchStatus(caseId, scenarioName, state,
			"actualization_update-errand-status---api-casedata-patch-status",
			equalToJson("""
				  {
				    "statusType": "Under granskning",
				    "description": "Under granskning",
				    "created": "${json-unit.any-string}"
				  }
				"""));
	}

	public static String mockActualizationCheckPhaseAction(String caseId, String scenarioName, String requiredScenarioState) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_check-phase-action_task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_COMPLETE,
				"displayPhaseParameter", "Granskning"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"actualization_check-phase-action_task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Aktualisering", PHASE_ACTION_COMPLETE, "COMPLETED", "Granskning")));
	}

}

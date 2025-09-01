package apptest.mock;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchStatus;
import static apptest.mock.api.Citizen.mockGetCitizen;
import static apptest.mock.api.Citizen.mockGetCitizenNoContent;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_COMPLETE;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;

import java.util.Map;

public class Actualization {

	private static final String MUNICIPALITY_ID = "2281";

	public static String mockActualization(String caseId, String scenarioName) {
		var scenarioAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, "check_appeal_check-appeal-task-worker---api-casedata-get-errand");
		var scenarioAfterVerifyResident = mockActualizationVerifyResident(caseId, scenarioName, scenarioAfterUpdatePhase, "2281");
		var scenarioAfterVerifyReporter = mockActualizationVerifyReporterStakeholder(caseId, scenarioName, scenarioAfterVerifyResident);
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

	public static String mockActualizationVerifyResident(String caseId, String scenarioName, String requiredScenarioState, String municipalityId) {
		var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"verify-resident-of-municipality-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Registrerad"));

		if (!MUNICIPALITY_ID.equals(municipalityId)) {
			return mockGetCitizenNoContent(scenarioName, state,
				"verify-resident-of-municipality-task-worker---api-citizen-getcitizen",
				"6b8928bb-9800-4d52-a9fa-20d88c81f1d6");
		}

		return mockGetCitizen(scenarioName, state,
			"verify-resident-of-municipality-task-worker---api-citizen-getcitizen",
			Map.of("municipalityId", municipalityId,
				"personId", "6b8928bb-9800-4d52-a9fa-20d88c81f1d6"));

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

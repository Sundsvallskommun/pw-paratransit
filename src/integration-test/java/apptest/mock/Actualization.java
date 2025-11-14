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

public class Actualization {

	public static String mockActualization(final String caseId, final String scenarioName) {
		final var scenarioAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, "check_appeal_check-appeal-task-worker---api-casedata-get-errand");
		final var scenarioAfterVerifyStatus = mockActualizationVerifyStatus(caseId, scenarioName, scenarioAfterUpdatePhase);
		final var scenarioAfterVerifyReporter = mockActualizationVerifyReporterStakeholder(caseId, scenarioName, scenarioAfterVerifyStatus);
		final var scenarioAfterUpdateDisplayPhase = mockActualizationUpdateDisplayPhase(caseId, scenarioName, scenarioAfterVerifyReporter);
		final var scenarioAfterUpdateStatus = mockActualizationUpdateStatus(caseId, scenarioName, scenarioAfterUpdateDisplayPhase);
		return mockActualizationCheckPhaseAction(caseId, scenarioName, scenarioAfterUpdateStatus);
	}

	public static String mockActualizationUpdatePhase(final String caseId, final String scenarioName, final String requiredScenarioState) {

		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"displayPhaseParameter", "Aktualisering",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN));

		var stateAfterErrandPatch = mockCaseDataPatch(caseId, scenarioName, state,
			"actualization_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchErrandBody("Aktualisering")));

		return mockCaseDataPatchExtraParameters(caseId, scenarioName, stateAfterErrandPatch,
		"actualization_update-phase-task-worker---api-casedata-patch-extra-parameters",
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
				        "values":["Registrerad"]
				    }
				]
				"""));
	}

	public static String mockActualizationVerifyStatus(final String caseId, final String scenarioName, final String requiredScenarioState) {
		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_verify-status---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"statusParameter", "Ärende inkommit",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Registrerad"));
	}

	public static String mockActualizationVerifyReporterStakeholder(final String caseId, final String scenarioName, final String requiredScenarioState) {
		return mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_verify-reporter-stakeholder---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Registrerad"));
	}

	public static String mockActualizationUpdateDisplayPhase(final String caseId, final String scenarioName, final String requiredScenarioState) {
		final var stateAfterGetErrand = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_update-display-phase---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_UNKNOWN,
				"displayPhaseParameter", "Registrerad"));

		final var stateAfterPatchErrand = mockCaseDataPatch(caseId, scenarioName, stateAfterGetErrand,
			"actualization_update-display-phase---api-casedata-patch-errand",
			equalToJson(createPatchErrandBody("Aktualisering")));

		return mockCaseDataPatchExtraParameters(caseId, scenarioName, stateAfterPatchErrand,
			"actualization_update-display-phase-task-worker---api-casedata-patch-extra-parameters",
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
				        "values":["Granskning"]
				    }
				]
				"""));
	}

	public static String mockActualizationUpdateStatus(final String caseId, final String scenarioName, final String requiredScenarioState) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
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

	public static String mockActualizationCheckPhaseAction(final String caseId, final String scenarioName, final String requiredScenarioState) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"actualization_check-phase-action_task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", PHASE_ACTION_COMPLETE,
				"displayPhaseParameter", "Granskning"));

		return mockCaseDataPatchExtraParameters(caseId, scenarioName, state,
			"actualization_check-phase-action-task-worker---api-casedata-patch-extra-parameters",
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
				        "values":["Granskning"]
				    }
				]
				"""));
	}

}

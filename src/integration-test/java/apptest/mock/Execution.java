package apptest.mock;

import static apptest.mock.api.CaseData.createPatchBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.Messaging.mockMessagingWebMessagePost;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;

import java.util.Map;

public class Execution {

	public static String mockExecution(String caseId, String scenarioName) {

		final var stateAfterUpdatePhase = mockExecutionUpdatePhase(caseId, scenarioName, "check-decision-task-worker---api-casedata-get-errand");
		return mockSendSimplifiedService(caseId, scenarioName, stateAfterUpdatePhase);
	}

	public static String mockExecutionUpdatePhase(String caseId, String scenarioName, String requiredScenarioState) {

		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut"));

		return mockCaseDataPatch(caseId, scenarioName, state,
			"execution_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchBody("Verkställa", PHASE_ACTION_UNKNOWN, "ONGOING", "Verkställa")));
	}

	public static String mockSendSimplifiedService(final String caseId, final String scenarioName, String requiredScenarioState) {
		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_send-simplified-service-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut"));

		mockMessagingWebMessagePost(
			equalToJson("""
							{
								"party" : {
									"partyId" : "6b8928bb-9800-4d52-a9fa-20d88c81f1d6",
									"externalReferences" : [ {
										"key" : "flowInstanceId",
										"value" : "2971"
									} ]
				      			},
				      			"message" : "Kontrollmeddelande för förenklad delgivning\\n\\nVi har nyligen delgivit dig ett beslut via brev. Du får nu ett kontrollmeddelande för att säkerställa att du mottagit informationen.\\nNär det har gått två veckor från det att beslutet skickades anses du blivit delgiven och du har då tre veckor på dig att överklaga beslutet.\\nOm du bara fått kontrollmeddelandet men inte själva delgivningen med beslutet måste du kontakta oss via e-post till\\nkontakt@sundsvall.se eller telefon till 060-19 10 00.",
				      			"sendAsOwner" : false,
				                "oepInstance" : "EXTERNAL",
				                "attachments" : [ ]
				    		}
				"""));
		return state;
	}
}

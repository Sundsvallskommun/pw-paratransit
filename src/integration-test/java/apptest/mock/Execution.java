package apptest.mock;

import static apptest.mock.api.CaseData.createPatchErrandBody;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchExtraParameters;
import static apptest.mock.api.Messaging.mockMessagingWebMessagePost;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;

import java.util.Map;

public class Execution {

	public static String mockExecution(final String caseId, final String scenarioName) {

		final var stateAfterUpdatePhase = mockExecutionUpdatePhase(caseId, scenarioName, "check-decision-task-worker---api-casedata-get-errand");
		return mockSendSimplifiedService(caseId, scenarioName, stateAfterUpdatePhase);
	}

	public static String mockExecutionUpdatePhase(final String caseId, final String scenarioName, final String requiredScenarioState) {

		final var state = mockCaseDataGet(caseId, scenarioName, requiredScenarioState,
			"execution_update-phase-task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "FINAL",
				"phaseParameter", "Beslut",
				"phaseActionParameter", "",
				"phaseStatusParameter", "",
				"displayPhaseParameter", "Beslut"));

		final var stateAfterErrandPatch = mockCaseDataPatch(caseId, scenarioName, state,
			"execution_update-phase-task-worker---api-casedata-patch-errand",
			equalToJson(createPatchErrandBody("Verkställa")));

		return mockCaseDataPatchExtraParameters(caseId, scenarioName, stateAfterErrandPatch,
			"execution_update-phase-task-worker---api-casedata-patch-extra-parameters",
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
				        "values":["Verkställa"]
				    }
				]
				"""));
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

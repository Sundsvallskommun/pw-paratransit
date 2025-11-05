package apptest;

import static apptest.mock.Actualization.mockActualizationCheckPhaseAction;
import static apptest.mock.Actualization.mockActualizationUpdateDisplayPhase;
import static apptest.mock.Actualization.mockActualizationUpdatePhase;
import static apptest.mock.Actualization.mockActualizationUpdateStatus;
import static apptest.mock.Actualization.mockActualizationVerifyReporterStakeholder;
import static apptest.mock.Actualization.mockActualizationVerifyStatus;
import static apptest.mock.Canceled.mockCanceled;
import static apptest.mock.CheckAppeal.mockCheckAppeal;
import static apptest.mock.Decision.mockDecision;
import static apptest.mock.Execution.mockExecution;
import static apptest.mock.FollowUp.mockFollowUp;
import static apptest.mock.Investigation.mockInvestigation;
import static apptest.mock.api.ApiGateway.mockApiGatewayToken;
import static apptest.mock.api.CaseData.mockCaseDataGet;
import static apptest.mock.api.CaseData.mockCaseDataPatch;
import static apptest.mock.api.CaseData.mockCaseDataPatchExtraParameters;
import static apptest.verification.ProcessPathway.canceledPathway;
import static apptest.verification.ProcessPathway.decisionPathway;
import static apptest.verification.ProcessPathway.executionPathway;
import static apptest.verification.ProcessPathway.followUpPathway;
import static apptest.verification.ProcessPathway.handlingPathway;
import static apptest.verification.ProcessPathway.investigationPathway;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static java.time.Duration.ZERO;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.tuple;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultPollDelay;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.awaitility.Awaitility.setDefaultTimeout;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static se.sundsvall.paratransit.Constants.CASE_TYPE_PARATRANSIT;

import apptest.verification.Tuples;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.paratransit.Application;
import se.sundsvall.paratransit.api.model.StartProcessResponse;

@DirtiesContext
@WireMockAppTestSuite(files = "classpath:/Wiremock/", classes = Application.class)
class ProcessWithActualizationDeviationIT extends AbstractCamundaAppTest {

	private static final int DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS = 30;
	private static final String TENANT_ID_PARATRANSIT = "PARATRANSIT";

	@BeforeEach
	void setup() {
		setDefaultPollInterval(500, MILLISECONDS);
		setDefaultPollDelay(ZERO);
		setDefaultTimeout(Duration.ofSeconds(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS));

		await()
			.ignoreExceptions()
			.atMost(DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS, SECONDS)
			.until(() -> camundaClient.getDeployments(null, null, TENANT_ID_PARATRANSIT).size(), equalTo(1));
	}

	@Test
	void test001_createProcessForCancelInActualization() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "789";
		var scenarioName = "test_actualization_001_createProcessForCancelInActualization";

		// Setup mocks
		mockApiGatewayToken();
		final var stateAfterCheckAppeal = mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARATRANSIT);
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, stateAfterCheckAppeal);
		final var stateAfterVerifyStatus = mockActualizationVerifyStatus(caseId, scenarioName, stateAfterUpdatePhase);
		final var stateAfterVerifyStakeholder = mockActualizationVerifyReporterStakeholder(caseId, scenarioName, stateAfterVerifyStatus);
		final var stateAfterUpdateDisplayPhase = mockActualizationUpdateDisplayPhase(caseId, scenarioName, stateAfterVerifyStakeholder);
		final var stateAfterUpdateStatus = mockActualizationUpdateStatus(caseId, scenarioName, stateAfterUpdateDisplayPhase);

		final var stateAfterGetErrand = mockCaseDataGet(caseId, scenarioName, stateAfterUpdateStatus,
			"actualization_check-phase-action_task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "CANCEL",
				"displayPhaseParameter", "Granskning"));

		final var stateAfterPatchExtraParameters = mockCaseDataPatchExtraParameters(caseId, scenarioName, stateAfterGetErrand,
			"actualization_check-phase-action-task-worker---api-casedata-patch-extra-parameters",
			equalToJson("""
				 [
				    {
				        "key":"process.displayPhase",
				        "values":["Granskning"]
				    },
				    {
				        "key":"process.phaseStatus",
				        "values":["CANCELED"]
				    },
				    {
				        "key":"process.phaseAction",
				        "values":["CANCEL"]
					}
				]
				"""));

		mockCanceled(caseId, scenarioName, stateAfterPatchExtraParameters);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertProcessPathway(startResponse.getProcessId(), false, Tuples.create()
			.with(tuple("Start process", "start_process"))
			.with(tuple("Check appeal", "external_task_check_appeal"))
			.with(tuple("Gateway isAppeal", "gateway_is_appeal"))
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Verify status", "external_task_actualization_verify_status"))
			.with(tuple("Wait if status is 'Utkast'", "gateway_actualization_verify_status"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Verify that reporter stakeholder exists", "external_task_actualization_verify_reporter_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role REPORTER assigned", "gateway_actualization_stakeholder_reporter_is_assigned"))
			.with(tuple("Update displayPhase", "external_task_actualization_update_display_phase"))
			.with(tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"))
			.with(tuple("Check phase action", "external_task_actualization_check_phase_action_task"))
			.with(tuple("Is phase action complete?", "gateway_actualization_is_phase_action_complete"))
			.with(tuple("End when canceled", "end_actualization_canceled"))
			.with(tuple("Is canceled in actualization", "gateway_actualization_canceled"))
			// Canceled
			.with(canceledPathway())
			.with(tuple("End process", "end_process")));
	}

	@Test
	void test002_createProcessForActualizationNotComplete() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "1011";
		final var scenarioName = "test_actualization_002_createProcessForActualizationNotComplete";

		// Setup mocks
		mockApiGatewayToken();
		final var stateAfterCheckAppeal = mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARATRANSIT);
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, stateAfterCheckAppeal);
		final var stateAfterVerifyStatus = mockActualizationVerifyStatus(caseId, scenarioName, stateAfterUpdatePhase);
		final var stateAfterVerifyStakeholder = mockActualizationVerifyReporterStakeholder(caseId, scenarioName, stateAfterVerifyStatus);
		final var stateAfterUpdateDisplayPhase = mockActualizationUpdateDisplayPhase(caseId, scenarioName, stateAfterVerifyStakeholder);
		final var stateAfterUpdateStatus = mockActualizationUpdateStatus(caseId, scenarioName, stateAfterUpdateDisplayPhase);

		final var stateAfterGetErrandNonComplete = mockCaseDataGet(caseId, scenarioName, stateAfterUpdateStatus,
			"actualization_check-phase-action_task-worker---api-casedata-get-errand-non-complete",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Granskning"));

		final var stateAfterPatchExtraParameters = mockCaseDataPatchExtraParameters(caseId, scenarioName, stateAfterGetErrandNonComplete,
			"actualization_check-phase-action-task-worker---api-casedata-patch-extra-parameters",
			equalToJson("""
				 [
				    {
				        "key":"process.displayPhase",
				        "values":["Granskning"]
				    },
				    {
				        "key":"process.phaseStatus",
				        "values":["WAITING"]
				    },
				    {
				        "key":"process.phaseAction",
				        "values":["UNKNOWN"]
					}
				]
				"""));

		mockActualizationCheckPhaseAction(caseId, scenarioName, stateAfterPatchExtraParameters);

		// Normal mock
		mockInvestigation(caseId, scenarioName);
		mockDecision(caseId, scenarioName);
		mockExecution(caseId, scenarioName);
		mockFollowUp(caseId, scenarioName);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		awaitProcessState("actualization_is_case_update_available", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Update process
		setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertProcessPathway(startResponse.getProcessId(), true, Tuples.create()
			.with(tuple("Start process", "start_process"))
			.with(tuple("Check appeal", "external_task_check_appeal"))
			.with(tuple("Gateway isAppeal", "gateway_is_appeal"))
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Verify status", "external_task_actualization_verify_status"))
			.with(tuple("Wait if status is 'Utkast'", "gateway_actualization_verify_status"))
			.with(tuple("Verify that reporter stakeholder exists", "external_task_actualization_verify_reporter_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role REPORTER assigned", "gateway_actualization_stakeholder_reporter_is_assigned"))
			.with(tuple("Update displayPhase", "external_task_actualization_update_display_phase"))
			.with(tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"))
			.with(tuple("Check phase action", "external_task_actualization_check_phase_action_task"))
			.with(tuple("Is phase action complete?", "gateway_actualization_is_phase_action_complete"))
			// phase action is not complete
			.with(tuple("Wait for complete action", "actualization_is_case_update_available"))
			.with(tuple("Check phase action", "external_task_actualization_check_phase_action_task"))
			.with(tuple("Is phase action complete?", "gateway_actualization_is_phase_action_complete"))
			.with(tuple("End actualization phase", "end_actualization_phase"))
			.with(tuple("Is canceled in actualization", "gateway_actualization_canceled"))
			.with(investigationPathway())
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(executionPathway())
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}

	@Test
	void test003_createProcessForCancelInActualizationWhenVerifyingReporter() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "1920";
		var scenarioName = "test_actualization_003_createProcessForCancelInActualizationWhenVerifyingReporter";

		// Setup mocks
		mockApiGatewayToken();
		final var stateAfterCheckAppeal = mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARATRANSIT);
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, stateAfterCheckAppeal);
		final var stateAfterVerifyStatus = mockActualizationVerifyStatus(caseId, scenarioName, stateAfterUpdatePhase);
		final var stateAfterGetCancelInVerifyStakeholder = mockCaseDataGet(caseId, scenarioName, stateAfterVerifyStatus,
			"actualization_verify-administrator-stakeholder--api-casedata-get-errand",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "CANCEL",
				"displayPhaseParameter", "Registrerad"));

		final var stateAfterPatchExtraParameters = mockCaseDataPatchExtraParameters(caseId, scenarioName, stateAfterGetCancelInVerifyStakeholder,
			"actualization_check-phase-action-task-worker---api-casedata-patch-extra-parameters",
			equalToJson("""
				 [
				    {
				        "key":"process.displayPhase",
				        "values":["Registrerad"]
				    },
				    {
				        "key":"process.phaseStatus",
				        "values":["CANCELED"]
				    },
				    {
				        "key":"process.phaseAction",
				        "values":["CANCEL"]
					}
				]
				"""));

		mockCanceled(caseId, scenarioName, stateAfterPatchExtraParameters);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertProcessPathway(startResponse.getProcessId(), false, Tuples.create()
			.with(tuple("Start process", "start_process"))
			.with(tuple("Check appeal", "external_task_check_appeal"))
			.with(tuple("Gateway isAppeal", "gateway_is_appeal"))
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Verify status", "external_task_actualization_verify_status"))
			.with(tuple("Wait if status is 'Utkast'", "gateway_actualization_verify_status"))
			.with(tuple("Verify that reporter stakeholder exists", "external_task_actualization_verify_reporter_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role REPORTER assigned", "gateway_actualization_stakeholder_reporter_is_assigned"))
			.with(tuple("End when canceled", "end_actualization_canceled"))
			.with(tuple("Is canceled in actualization", "gateway_actualization_canceled"))
			// Canceled
			.with(canceledPathway())
			.with(tuple("End process", "end_process")));
	}

	@Test
	void test004_createProcessWaitingForStakeholderUpdateInActualization() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "2021";
		final var scenarioName = "test_actualization_004_createProcessWaitingForStakeholderUpdateInActualization(";

		// Setup mocks
		mockApiGatewayToken();
		final var stateAfterCheckAppeal = mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARATRANSIT);
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, stateAfterCheckAppeal);
		final var stateAfterVerifyStatus = mockActualizationVerifyStatus(caseId, scenarioName, stateAfterUpdatePhase);
		final var stateAfterVerifyStakeholderNoReporter = mockCaseDataGet(caseId, scenarioName, stateAfterVerifyStatus,
			"actualization_verify-administrator-stakeholder---api-casedata-get-errand-no-administrator",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"displayPhaseParameter", "Registrerad"), "APPROVAL", "APPLICANT");

		final var stateAfterPatchExtraParameters = mockCaseDataPatchExtraParameters(caseId, scenarioName, stateAfterVerifyStakeholderNoReporter,
			"actualization_verify-administrator-stakeholder---api-casedata-patch-extra-parameters",
			equalToJson("""
				 [
				    {
				        "key":"process.displayPhase",
				        "values":["Registrerad"]
				    },
				    {
				        "key":"process.phaseStatus",
				        "values":["CANCELED"]
				    },
				    {
				        "key":"process.phaseAction",
				        "values":["CANCEL"]
					}
				]
				"""));

		final var stateAfterVerifyStakeholder = mockActualizationVerifyReporterStakeholder(caseId, scenarioName, stateAfterPatchExtraParameters);
		final var stateAfterUpdateDisplayPhase = mockActualizationUpdateDisplayPhase(caseId, scenarioName, stateAfterVerifyStakeholder);
		final var stateAfterUpdateStatus = mockActualizationUpdateStatus(caseId, scenarioName, stateAfterUpdateDisplayPhase);

		mockActualizationCheckPhaseAction(caseId, scenarioName, stateAfterUpdateStatus);

		// Normal mock
		mockInvestigation(caseId, scenarioName);
		mockDecision(caseId, scenarioName);
		mockExecution(caseId, scenarioName);
		mockFollowUp(caseId, scenarioName);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		awaitProcessState("actualization_wait_for_stakeholder_update", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Update process
		setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertProcessPathway(startResponse.getProcessId(), true, Tuples.create()
			.with(tuple("Start process", "start_process"))
			.with(tuple("Check appeal", "external_task_check_appeal"))
			.with(tuple("Gateway isAppeal", "gateway_is_appeal"))
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Verify status", "external_task_actualization_verify_status"))
			.with(tuple("Wait if status is 'Utkast'", "gateway_actualization_verify_status"))
			.with(tuple("Verify that reporter stakeholder exists", "external_task_actualization_verify_reporter_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role REPORTER assigned", "gateway_actualization_stakeholder_reporter_is_assigned"))
			// Stakeholder Reporter is not assigned
			.with(tuple("Wait for case update", "actualization_wait_for_stakeholder_update"))
			// Stakeholder update
			.with(tuple("Verify that reporter stakeholder exists", "external_task_actualization_verify_reporter_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role REPORTER assigned", "gateway_actualization_stakeholder_reporter_is_assigned"))
			.with(tuple("Update displayPhase", "external_task_actualization_update_display_phase"))
			.with(tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"))
			.with(tuple("Check phase action", "external_task_actualization_check_phase_action_task"))
			.with(tuple("Is phase action complete?", "gateway_actualization_is_phase_action_complete"))
			.with(tuple("End actualization phase", "end_actualization_phase"))
			.with(tuple("Is canceled in actualization", "gateway_actualization_canceled"))
			.with(investigationPathway())
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(executionPathway())
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}

	@Test
	void test005_createProcessForCancelInVerifyStatus() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "789";
		var scenarioName = "test_actualization_005_createProcessForCancelInVerifyStatus";

		// Setup mocks
		mockApiGatewayToken();
		final var stateAfterCheckAppeal = mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARATRANSIT);
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, stateAfterCheckAppeal);

		final var stateAfterGetErrand = mockCaseDataGet(caseId, scenarioName, stateAfterUpdatePhase,
			"actualization_verify_status_task-worker---api-casedata-get-errand",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "CANCEL",
				"displayPhaseParameter", "Granskning"));

		final var stateAfterPatchExtraParameters = mockCaseDataPatchExtraParameters(caseId, scenarioName, stateAfterGetErrand,
			"actualization_verify_status_task-worker---api-casedata-patch-errand-extra-parameters",
			equalToJson("""
				 [
				    {
				        "key":"process.phaseStatus",
				        "values":["CANCELED"]
				    },
				    {
				        "key":"process.phaseAction",
				        "values":["CANCEL"]
					}
				]
				"""));

		mockCanceled(caseId, scenarioName, stateAfterPatchExtraParameters);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertProcessPathway(startResponse.getProcessId(), false, Tuples.create()
			.with(tuple("Start process", "start_process"))
			.with(tuple("Check appeal", "external_task_check_appeal"))
			.with(tuple("Gateway isAppeal", "gateway_is_appeal"))
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Verify status", "external_task_actualization_verify_status"))
			.with(tuple("Wait if status is 'Utkast'", "gateway_actualization_verify_status"))
			.with(tuple("End when canceled", "end_actualization_canceled"))
			.with(tuple("Is canceled in actualization", "gateway_actualization_canceled"))
			// Canceled
			.with(canceledPathway())
			.with(tuple("End process", "end_process")));
	}

	@Test
	void test006_createProcessWaitingForStatusUpdateInActualization() throws JsonProcessingException, ClassNotFoundException {

		final var caseId = "2021";
		final var scenarioName = "test_actualization_test006_createProcessWaitingForStatusUpdateInActualization";

		// Setup mocks
		mockApiGatewayToken();
		final var stateAfterCheckAppeal = mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARATRANSIT);
		final var stateAfterUpdatePhase = mockActualizationUpdatePhase(caseId, scenarioName, stateAfterCheckAppeal);
		final var stateAfterVerifyStatusDraft = mockCaseDataGet(caseId, scenarioName, stateAfterUpdatePhase,
			"actualization_verify-status_task_worker---api-casedata-get-errand-no-administrator",
			Map.of("decisionTypeParameter", "PROPOSED",
				"phaseParameter", "Aktualisering",
				"phaseStatusParameter", "ONGOING",
				"phaseActionParameter", "UNKNOWN",
				"statusParameter", "Utkast",
				"displayPhaseParameter", "Registrerad"), "APPROVAL", "APPLICANT");

		final var stateAfterPatchExtraParameters = mockCaseDataPatchExtraParameters(caseId, scenarioName, stateAfterVerifyStatusDraft,
			"actualization_verify_status_task-worker---api-casedata-patch-errand-extra-parameters",
			equalToJson("""
				 [
				    {
				        "key":"process.phaseStatus",
				        "values":["WAITING"]
				    },
				    {
				        "key":"process.phaseAction",
				        "values":["UNKNOWN"]
					}
				]
				"""));
		final var stateAfterChangedStatus = mockActualizationVerifyStatus(caseId, scenarioName, stateAfterPatchExtraParameters);
		final var stateAfterVerifyStakeholder = mockActualizationVerifyReporterStakeholder(caseId, scenarioName, stateAfterChangedStatus);
		final var stateAfterUpdateDisplayPhase = mockActualizationUpdateDisplayPhase(caseId, scenarioName, stateAfterVerifyStakeholder);
		final var stateAfterUpdateStatus = mockActualizationUpdateStatus(caseId, scenarioName, stateAfterUpdateDisplayPhase);

		mockActualizationCheckPhaseAction(caseId, scenarioName, stateAfterUpdateStatus);

		// Normal mock
		mockInvestigation(caseId, scenarioName);
		mockDecision(caseId, scenarioName);
		mockExecution(caseId, scenarioName);
		mockFollowUp(caseId, scenarioName);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + caseId)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for process to be waiting for update of errand
		awaitProcessState("actualization_wait_for_status_update", DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Update process
		setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/update/" + startResponse.getProcessId())
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.withExpectedResponseBodyIsNull()
			.sendRequest();

		// Wait for process to finish
		awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Verify wiremock stubs
		verifyAllStubs();

		// Verify process pathway.
		assertProcessPathway(startResponse.getProcessId(), true, Tuples.create()
			.with(tuple("Start process", "start_process"))
			.with(tuple("Check appeal", "external_task_check_appeal"))
			.with(tuple("Gateway isAppeal", "gateway_is_appeal"))
			// Actualization
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Verify status", "external_task_actualization_verify_status"))
			// Status is 'Utkast'
			.with(tuple("Wait if status is 'Utkast'", "gateway_actualization_verify_status"))
			.with(tuple("Wait for case update", "actualization_wait_for_status_update"))
			// Status update
			.with(tuple("Verify status", "external_task_actualization_verify_status"))
			.with(tuple("Wait if status is 'Utkast'", "gateway_actualization_verify_status"))
			.with(tuple("Verify that reporter stakeholder exists", "external_task_actualization_verify_reporter_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role REPORTER assigned", "gateway_actualization_stakeholder_reporter_is_assigned"))
			.with(tuple("Update displayPhase", "external_task_actualization_update_display_phase"))
			.with(tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"))
			.with(tuple("Check phase action", "external_task_actualization_check_phase_action_task"))
			.with(tuple("Is phase action complete?", "gateway_actualization_is_phase_action_complete"))
			.with(tuple("End actualization phase", "end_actualization_phase"))
			.with(tuple("Is canceled in actualization", "gateway_actualization_canceled"))
			.with(investigationPathway())
			.with(tuple("Is canceled in investigation", "gateway_investigation_canceled"))
			.with(decisionPathway())
			.with(tuple("Is canceled in decision or not approved", "gateway_decision_canceled"))
			.with(handlingPathway())
			.with(executionPathway())
			.with(followUpPathway())
			.with(tuple("End process", "end_process")));
	}
}

package apptest;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.paratransit.Application;
import se.sundsvall.paratransit.api.model.StartProcessResponse;

import static apptest.mock.Actualization.mockActualization;
import static apptest.mock.CheckAppeal.mockCheckAppeal;
import static apptest.mock.Decision.mockDecision;
import static apptest.mock.Execution.mockExecution;
import static apptest.mock.FollowUp.mockFollowUp;
import static apptest.mock.Investigation.mockInvestigation;
import static apptest.mock.api.ApiGateway.mockApiGatewayToken;
import static java.time.Duration.ZERO;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultPollDelay;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.awaitility.Awaitility.setDefaultTimeout;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static se.sundsvall.paratransit.Constants.CASE_TYPE_PARATRANSIT;

/**
 * Runs the standard happy-path flow with {@code process-engine.type=operaton}, so the worker write-path
 * (clearUpdateAvailable -&gt; OperatonEngineClient -&gt; OperatonClient) executes against a live engine. The engine
 * container is shared with the Camunda flow tests since Operaton is API-compatible with Camunda 7 - this verifies the
 * Operaton wiring, not the WSO2 transport (see DEL4 for the external-task-client OAuth against the gateway).
 */
@DirtiesContext
@WireMockAppTestSuite(files = "classpath:/Wiremock/", classes = Application.class)
class ProcessWithoutDeviationOperatonIT extends AbstractCamundaAppTest {

	private static final int DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS = 30;
	private static final String TENANT_ID_PARATRANSIT = "PARATRANSIT";

	@DynamicPropertySource
	static void operatonEngine(DynamicPropertyRegistry registry) {
		registry.add("process-engine.type", () -> "operaton");
	}

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
	void test001_createProcessForCitizenWithOperatonEngine() throws ClassNotFoundException {

		final var caseId = "123";
		final var scenarioName = "test001_createProcessForCitizen";

		// Setup mocks
		mockApiGatewayToken();
		mockCheckAppeal(caseId, scenarioName, CASE_TYPE_PARATRANSIT);
		mockActualization(caseId, scenarioName);
		mockInvestigation(caseId, scenarioName);
		mockDecision(caseId, scenarioName);
		mockExecution(caseId, scenarioName);
		mockFollowUp(caseId, scenarioName);

		// Start process
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/123")
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		// Wait for the process to finish - completion requires every worker (incl. the Operaton-routed
		// clearUpdateAvailable) to succeed against the engine.
		awaitProcessCompleted(startResponse.getProcessId(), DEFAULT_TESTCASE_TIMEOUT_IN_SECONDS);

		// Verify wiremock stubs
		verifyAllStubs();
	}
}

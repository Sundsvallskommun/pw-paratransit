package apptest;

import static generated.se.sundsvall.camunda.HistoricProcessInstanceDto.StateEnum.ACTIVE;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.awaitility.Awaitility.await;
import static org.awaitility.Awaitility.setDefaultPollDelay;
import static org.awaitility.Awaitility.setDefaultPollInterval;
import static org.awaitility.Awaitility.setDefaultTimeout;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.ACCEPTED;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import se.sundsvall.dept44.test.annotation.wiremock.WireMockAppTestSuite;
import se.sundsvall.paratransit.Application;
import se.sundsvall.paratransit.api.model.StartProcessResponse;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;

@WireMockAppTestSuite(files = "classpath:/CreateProcess/", classes = Application.class)
class CreateProcessIT extends AbstractCamundaAppTest {

	@Autowired
	private CamundaClient camundaClient;

	@BeforeEach
	void setup() {
		setDefaultPollInterval(500, MILLISECONDS);
		setDefaultPollDelay(Duration.ZERO);
		setDefaultTimeout(Duration.ofSeconds(30));

		await()
			.ignoreExceptions()
			.until(() -> camundaClient.getDeployments("process-paratransit.bpmn", null, null).size(), equalTo(1));

		verifyAllStubs();
	}

	@Test
	void test001_createProcessWithoutUpdates() throws JsonProcessingException, ClassNotFoundException {

		// === Start process ===
		final var startResponse = setupCall()
			.withServicePath("/2281/SBK_PARKING_PERMIT/process/start/" + 123L)
			.withHttpMethod(POST)
			.withExpectedResponseStatus(ACCEPTED)
			.sendRequest()
			.andReturnBody(StartProcessResponse.class);

		await()
			.until(() -> camundaClient.getHistoricProcessInstance(startResponse.getProcessId()).getState(), equalTo(ACTIVE));

		//TODO Fix real test when integrations and workers are in place
	}
}

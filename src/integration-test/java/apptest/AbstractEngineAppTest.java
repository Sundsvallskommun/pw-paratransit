package apptest;

import static generated.se.sundsvall.camunda.HistoricProcessInstanceDto.StateEnum.COMPLETED;
import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import generated.se.sundsvall.camunda.HistoricActivityInstanceDto;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import se.sundsvall.dept44.test.AbstractAppTest;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;

/**
 * Engine-neutral base for the testcontainer-driven process tests. Holds the shared assertion and await helpers; the
 * per-engine base classes (in the {@code apptest.camunda} and {@code apptest.operaton} packages) pick the engine by
 * delegating their {@code @DynamicPropertySource} to {@link apptest.engine.EngineTestProperties}. The
 * {@code camundaClient} is used purely as a read client for process history and works against either engine since
 * Operaton is API-compatible with Camunda 7.
 * See Camunda API for more details https://docs.camunda.org/rest/camunda-bpm-platform/7.20/
 */
public abstract class AbstractEngineAppTest extends AbstractAppTest {

	private static final String TENANT_ID_PARATRANSIT = "PARATRANSIT";
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	@Autowired
	protected CamundaClient camundaClient;

	@Value("${integration.camunda.url}")
	private String engineBaseUrl;

	/**
	 * Gives every test a clean slate on the shared engine container before it runs. The container is reused across
	 * {@code @DirtiesContext} contexts, so a process instance that outlived an earlier test (after a failure, or because
	 * the slower Operaton engine had not drained it yet) would otherwise be picked up by this context's external task
	 * workers and corrupt this test's WireMock scenario state. We delete the tenant's instances and then clear the
	 * WireMock request journal, so a stray request from a leftover worker racing the context startup cannot trip the
	 * {@code failFast} near-miss check before the test's own flow even begins.
	 */
	@BeforeEach
	void resetSharedEngineState() throws Exception {
		final var listRequest = HttpRequest.newBuilder(URI.create(engineBaseUrl + "/process-instance?tenantIdIn=" + TENANT_ID_PARATRANSIT)).GET().build();
		final var listResponse = HTTP_CLIENT.send(listRequest, HttpResponse.BodyHandlers.ofString());
		final JsonNode instances = OBJECT_MAPPER.readTree(listResponse.body());
		for (final JsonNode instance : instances) {
			final var deleteRequest = HttpRequest.newBuilder(
				URI.create(engineBaseUrl + "/process-instance/" + instance.get("id").asText() + "?skipCustomListeners=true&skipIoMappings=true&failIfNotExists=false"))
				.DELETE().build();
			HTTP_CLIENT.send(deleteRequest, HttpResponse.BodyHandlers.discarding());
		}
		wiremock.resetRequests();
	}

	protected List<HistoricActivityInstanceDto> getProcessInstanceRoute(String processInstanceId) {
		return getRoute(processInstanceId, new ArrayList<>());
	}

	private List<HistoricActivityInstanceDto> getRoute(String processInstanceId, List<HistoricActivityInstanceDto> route) {
		if (isNull(processInstanceId)) {
			return route;
		}
		return camundaClient.getHistoricActivities(processInstanceId).stream()
			.filter(e -> e.getEndTime() != null)
			.sorted(comparing(HistoricActivityInstanceDto::getEndTime))
			.flatMap(activity -> concat(Stream.of(activity), getRoute(activity.getCalledProcessInstanceId(), route).stream()))
			.toList();
	}

	protected void awaitProcessCompleted(String processId, long timeoutInSeconds) {
		await()
			.ignoreExceptions()
			.atMost(timeoutInSeconds, SECONDS)
			.failFast("Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getHistoricProcessInstance(processId).getState(), equalTo(COMPLETED));
	}

	protected void awaitProcessState(String state, long timeoutInSeconds) {
		await()
			.ignoreExceptions()
			.atMost(timeoutInSeconds, SECONDS)
			.failFast("Wiremock has mismatch!", () -> !wiremock.findNearMissesForUnmatchedRequests().getNearMisses().isEmpty())
			.until(() -> camundaClient.getEventSubscriptions().stream().filter(eventSubscription -> state.equals(eventSubscription.getActivityId())).count(), equalTo(1L));
	}

	protected void assertProcessPathway(String processId, boolean acceptDuplication, ArrayList<Tuple> list) {
		var element = assertThat(getProcessInstanceRoute(processId))
			.extracting(HistoricActivityInstanceDto::getActivityName, HistoricActivityInstanceDto::getActivityId)
			.containsExactlyInAnyOrderElementsOf(list);
		if (!acceptDuplication) {
			element.doesNotHaveDuplicates();
		}
	}
}

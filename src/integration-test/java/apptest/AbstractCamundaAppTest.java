package apptest;

import org.junit.jupiter.api.AfterAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import se.sundsvall.dept44.test.AbstractAppTest;

/**
 * Test class using testcontainer to execute the process.
 * There are a lot of resources that can be added to CamundaClient
 * to make good assertions. This test class contains a few examples.
 * See Camunda API for more details https://docs.camunda.org/rest/camunda-bpm-platform/7.20/
 */
@Testcontainers
abstract class AbstractCamundaAppTest extends AbstractAppTest {

	private static final String CAMUNDA_IMAGE_NAME = "camunda/camunda-bpm-platform:run-7.20.0";

	@Container
	private static final GenericContainer<?> CAMUNDA = new GenericContainer<>(CAMUNDA_IMAGE_NAME)
		.waitingFor(Wait.forHttp("/"))
		.withExposedPorts(8080);

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		CAMUNDA.start();
		final var camundaBaseUrl = ("http://" + "localhost:" + CAMUNDA.getMappedPort(8080) + "/engine-rest");
		registry.add("integration.camunda.url", () -> camundaBaseUrl);
		registry.add("camunda.bpm.client.base-url", () -> camundaBaseUrl);
	}

	@AfterAll
	static void teardown() {
		CAMUNDA.stop();
	}
}

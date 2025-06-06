package se.sundsvall.paratransit.api;

import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import se.sundsvall.paratransit.Application;
import se.sundsvall.paratransit.api.model.StartProcessResponse;
import se.sundsvall.paratransit.service.ProcessService;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("junit")
class ProcessResourceTest {

	static final String PATH = "/2281/SBK_PARKING_PERMIT";

	@MockitoBean
	private ProcessService processServiceMock;

	@Autowired
	private WebTestClient webTestClient;

	@LocalServerPort
	private int port;

	@Test
	void startProcess() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "SBK_PARKING_PERMIT";
		final var caseNumber = 123L;
		final var processInstanceId = randomUUID().toString();

		when(processServiceMock.startProcess(any(), any(), any())).thenReturn(processInstanceId);

		// Act
		final var response = webTestClient.post()
			.uri(PATH + "/process/start/" + caseNumber)
			.exchange()
			.expectStatus().isAccepted()
			.expectHeader().contentType(APPLICATION_JSON)
			.expectBody(StartProcessResponse.class)
			.returnResult()
			.getResponseBody();

		// Assert
		assertThat(response.getProcessId()).isEqualTo(processInstanceId);
		verify(processServiceMock).startProcess(municipalityId, namespace, caseNumber);
		verifyNoMoreInteractions(processServiceMock);
	}

	@Test
	void updateProcess() {

		// Arrange
		final var municipalityId = "2281";
		final var namespace = "SBK_PARKING_PERMIT";
		final var processInstanceId = randomUUID().toString();

		when(processServiceMock.startProcess(any(), any(), any())).thenReturn(processInstanceId);

		// Act
		webTestClient.post()
			.uri(PATH + "/process/update/" + processInstanceId)
			.exchange()
			.expectStatus().isAccepted()
			.expectBody().isEmpty();

		// Assert
		verify(processServiceMock).updateProcess(municipalityId, namespace, processInstanceId);
		verifyNoMoreInteractions(processServiceMock);
	}
}

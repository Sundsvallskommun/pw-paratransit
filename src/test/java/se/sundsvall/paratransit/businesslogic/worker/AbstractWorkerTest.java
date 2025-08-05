package se.sundsvall.paratransit.businesslogic.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;

import generated.se.sundsvall.camunda.VariableValueDto;
import generated.se.sundsvall.casedata.Attachment;
import java.util.ArrayList;
import java.util.UUID;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;

@ExtendWith(MockitoExtension.class)
class AbstractWorkerTest {

	private static class Worker extends AbstractWorker {

		protected Worker(CamundaClient camundaClient, CaseDataClient caseDataClient, FailureHandler failureHandler) {
			super(camundaClient, caseDataClient, failureHandler);
		}

		@Override
		protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
			// Do nothing
		}
	} // Test class extending the abstract class containing the clearUpdateAvailable method

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CaseDataClient caseDataClientMock;

	@InjectMocks
	private Worker worker;

	@Test
	void clearUpdateAvailable() {
		// Setup
		final var uuid = UUID.randomUUID().toString();
		final var key = "updateAvailable";
		final var value = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(false);

		// Mock
		when(externalTaskMock.getProcessInstanceId()).thenReturn(uuid);

		// Act
		worker.clearUpdateAvailable(externalTaskMock);

		// Assert and verify
		verify(camundaClientMock).setProcessInstanceVariable(uuid, key, value);
		verifyNoMoreInteractions(camundaClientMock);
	}

	@Test
	void execute() {
		final var requestId = UUID.randomUUID().toString();

		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(requestId);

		// Mock static RequestId to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			// Act
			worker.execute(externalTaskMock, externalTaskServiceMock);

			// Verify static method
			requestIdMock.verify(() -> RequestId.init(requestId));
		}
	}

	@Test
	void getErrandAttachments() {
		final var list = new ArrayList<Attachment>();
		final var municipalityId = "municipalityId";
		final var namespace = "namespace";
		final var caseNumber = 1L;
		when(caseDataClientMock.getErrandAttachments(any(), any(), any())).thenReturn(list);

		final var result = worker.getErrandAttachments(municipalityId, namespace, caseNumber);

		assertThat(result).isSameAs(list);
		verify(caseDataClientMock).getErrandAttachments(municipalityId, namespace, caseNumber);
	}
}

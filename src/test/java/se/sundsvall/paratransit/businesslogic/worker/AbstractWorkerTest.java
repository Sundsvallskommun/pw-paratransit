package se.sundsvall.paratransit.businesslogic.worker;

import generated.se.sundsvall.camunda.VariableValueDto;
import java.util.UUID;
import org.camunda.bpm.client.exception.RestException;
import org.camunda.bpm.client.exception.UnknownHttpErrorException;
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
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;
import se.sundsvall.paratransit.integration.engine.EngineClient;
import se.sundsvall.paratransit.integration.engine.externaltask.OperatonExternalTaskToken;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.paratransit.Constants.PROCESS_VARIABLE_REQUEST_ID;

@ExtendWith(MockitoExtension.class)
class AbstractWorkerTest {

	private static class Worker extends AbstractWorker {

		protected Worker(EngineClient engineClient, CaseDataClient caseDataClient, FailureHandler failureHandler, OperatonExternalTaskToken token) {
			super(engineClient, caseDataClient, failureHandler, token);
		}

		@Override
		protected void executeBusinessLogic(ExternalTask externalTask, ExternalTaskService externalTaskService) {
			externalTaskService.complete(externalTask);
		}
	} // Test class extending the abstract class, completing the task through the service it is given

	@Mock
	private EngineClient engineClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private OperatonExternalTaskToken tokenMock;

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
		verify(engineClientMock).setProcessInstanceVariable(uuid, key, value);
		verifyNoMoreInteractions(engineClientMock);
	}

	@Test
	void execute() {
		final var requestId = UUID.randomUUID().toString();

		when(externalTaskMock.getVariable(PROCESS_VARIABLE_REQUEST_ID)).thenReturn(requestId);

		// Mock static RequestId to verify that static method is being called
		try (MockedStatic<RequestId> requestIdMock = mockStatic(RequestId.class)) {
			// Act
			worker.execute(externalTaskMock, externalTaskServiceMock);

			// Verify static method
			requestIdMock.verify(() -> RequestId.init(requestId));
		}
		verify(externalTaskServiceMock).complete(externalTaskMock);
	}

	@Test
	void executeRetriesServiceCallOnUnauthorized() {
		final var restException = new RestException("message", "type", 0);
		restException.setHttpStatusCode(401);
		doThrow(new UnknownHttpErrorException("Unauthorized", restException)).doNothing().when(externalTaskServiceMock).complete(externalTaskMock);

		worker.execute(externalTaskMock, externalTaskServiceMock);

		verify(externalTaskServiceMock, times(2)).complete(externalTaskMock);
		verifyNoMoreInteractions(externalTaskServiceMock);
	}

}

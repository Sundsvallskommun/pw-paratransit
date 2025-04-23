package se.sundsvall.workflow.service;

import static java.util.Optional.empty;
import static java.util.UUID.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import generated.se.sundsvall.camunda.ProcessInstanceDto;
import generated.se.sundsvall.camunda.ProcessInstanceWithVariablesDto;
import generated.se.sundsvall.camunda.StartProcessInstanceDto;
import generated.se.sundsvall.camunda.VariableValueDto;
import java.util.Optional;
import org.apache.commons.lang3.RandomStringUtils;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.workflow.integration.camunda.CamundaClient;

@ExtendWith(MockitoExtension.class)
class ProcessServiceTest {

	@Mock
	private CamundaClient camundaClientMock;

	@InjectMocks
	private ProcessService processService;

	@Test
	void startProcess() {

		// Arrange
		final var process = "template-camunda-process";
		final var tenant = "TEMPLATE_NAMESPACE";
		final var businessKey = RandomStringUtils.secure().next(10);
		final var uuid = randomUUID().toString();
		final var processInstance = new ProcessInstanceWithVariablesDto().id(uuid);

		when(camundaClientMock.startProcessWithTenant(any(), any(), any())).thenReturn(processInstance);

		// Act
		final var processId = processService.startProcess(businessKey);

		// Assert
		assertThat(processId).isEqualTo(uuid);
		verify(camundaClientMock).startProcessWithTenant(process, tenant, new StartProcessInstanceDto().businessKey(businessKey));
		verifyNoMoreInteractions(camundaClientMock);
	}

	@Test
	void updateProcess() {

		// Arrange
		final var uuid = randomUUID().toString();
		final var key = "updateAvailable";
		final var value = new VariableValueDto().type(ValueType.BOOLEAN.getName()).value(true);

		when(camundaClientMock.getProcessInstance(any())).thenReturn(Optional.of(new ProcessInstanceDto()));

		// Act
		processService.updateProcess(uuid);

		// Assert
		verify(camundaClientMock).getProcessInstance(uuid);
		verify(camundaClientMock).setProcessInstanceVariable(uuid, key, value);
		verifyNoMoreInteractions(camundaClientMock);
	}

	@Test
	void updateProcessNotFound() {

		// Arrange
		final var uuid = randomUUID().toString();

		when(camundaClientMock.getProcessInstance(any())).thenReturn(empty());

		// Act
		final var result = assertThrows(ThrowableProblem.class, () -> processService.updateProcess(uuid));

		// Assert
		assertThat(result)
			.hasFieldOrPropertyWithValue("status", Status.NOT_FOUND)
			.hasFieldOrPropertyWithValue("detail", "Process instance with ID '%s' does not exist!".formatted(uuid));

		verify(camundaClientMock).getProcessInstance(uuid);
		verify(camundaClientMock, never()).setProcessInstanceVariable(any(), any(), any());
		verifyNoMoreInteractions(camundaClientMock);
	}
}

package se.sundsvall.paratransit.businesslogic.worker.actualization;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_HAS_ADMINISTRATOR_AND_APPLICANT;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_PHASE_STATUS;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_UPDATE_AVAILABLE;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_STATUS;
import static se.sundsvall.paratransit.Constants.FALSE;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_CANCEL;
import static se.sundsvall.paratransit.Constants.PHASE_ACTION_UNKNOWN;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_CANCELED;
import static se.sundsvall.paratransit.Constants.PHASE_STATUS_WAITING;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Stakeholder;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;

@ExtendWith(MockitoExtension.class)
class VerifyStakeholdersExistsTaskWorkerTest {

	private static final String PROCESS_INSTANCE_ID = UUID.randomUUID().toString();
	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";
	private static final String DISPLAY_PHASE = "displayPhase";

	@Mock
	private CamundaClient camundaClientMock;

	@Mock
	private CaseDataClient caseDataClientMock;

	@Mock
	private ExternalTask externalTaskMock;

	@Mock
	private ExternalTaskService externalTaskServiceMock;

	@Mock
	private FailureHandler failureHandlerMock;

	@Mock
	private Errand errandMock;

	@InjectMocks
	private VerifyStakeholdersExistsTaskWorker worker;

	@Captor
	private ArgumentCaptor<List<ExtraParameter>> patchCaptor;

	@Captor
	private ArgumentCaptor<Map<String, Object>> variablesCaptor;

	@BeforeEach
	void commonMocking() {
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getProcessInstanceId()).thenReturn(PROCESS_INSTANCE_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
	}

	@Test
	void executeWhenErrandHasAdministratorAndApplicant() {
		// Arrange
		when(errandMock.getStakeholders()).thenReturn(List.of(
			new Stakeholder().roles(List.of("ADMINISTRATOR")),
			new Stakeholder().roles(List.of("APPLICANT"))));

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(errandMock, times(2)).getStakeholders();
		verify(errandMock).getExtraParameters();
		verify(externalTaskServiceMock).complete(eq(externalTaskMock), variablesCaptor.capture());
		verifyNoMoreInteractions(camundaClientMock, caseDataClientMock, errandMock, externalTaskMock, externalTaskServiceMock);
		verifyNoInteractions(failureHandlerMock);

		assertThat(variablesCaptor.getValue()).containsExactlyEntriesOf(
			Map.of(CAMUNDA_VARIABLE_HAS_ADMINISTRATOR_AND_APPLICANT, true));
	}

	@ParameterizedTest
	@MethodSource("stakeholderLists")
	void executeWhenErrandHasNotApplicantAndAdministrator(List<Stakeholder> stakeholders) {
		when(errandMock.getStakeholders()).thenReturn(stakeholders);
		when(errandMock.getId()).thenReturn(ERRAND_ID);
		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(errandMock, times(2)).getStakeholders();
		verify(errandMock).getExtraParameters();
		verify(caseDataClientMock).updateExtraParameters(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		verify(externalTaskServiceMock).complete(eq(externalTaskMock), variablesCaptor.capture());
		verifyNoMoreInteractions(camundaClientMock, caseDataClientMock, errandMock, externalTaskMock, externalTaskServiceMock);
		verifyNoInteractions(failureHandlerMock);

		assertThat(patchCaptor.getValue()).extracting(ExtraParameter::getKey, ExtraParameter::getValues).containsExactlyInAnyOrder(
			tuple(CASEDATA_KEY_PHASE_STATUS, List.of(PHASE_STATUS_WAITING)),
			tuple(CASEDATA_KEY_PHASE_ACTION, List.of(PHASE_ACTION_UNKNOWN)));

		assertThat(variablesCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(
			Map.of(CAMUNDA_VARIABLE_HAS_ADMINISTRATOR_AND_APPLICANT, false,
				CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_WAITING));
	}

	@Test
	void executeWhenCanceled() {
		// Arrange
		when(errandMock.getExtraParameters()).thenReturn(List.of(new ExtraParameter(CASEDATA_KEY_DISPLAY_PHASE).addValuesItem(DISPLAY_PHASE),
			new ExtraParameter(CASEDATA_KEY_PHASE_ACTION).addValuesItem(PHASE_ACTION_CANCEL)));
		when(errandMock.getId()).thenReturn(ERRAND_ID);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(errandMock, times(2)).getStakeholders();
		verify(errandMock).getExtraParameters();
		verify(caseDataClientMock).updateExtraParameters(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), patchCaptor.capture());
		verify(externalTaskServiceMock).complete(eq(externalTaskMock), variablesCaptor.capture());
		verifyNoMoreInteractions(camundaClientMock, caseDataClientMock, errandMock, externalTaskMock, externalTaskServiceMock);
		verifyNoInteractions(failureHandlerMock);

		assertThat(patchCaptor.getValue()).extracting(ExtraParameter::getKey, ExtraParameter::getValues).containsExactlyInAnyOrder(
			tuple(CASEDATA_KEY_PHASE_STATUS, List.of(PHASE_STATUS_CANCELED)),
			tuple(CASEDATA_KEY_PHASE_ACTION, List.of(PHASE_ACTION_CANCEL)));

		assertThat(variablesCaptor.getValue()).containsExactlyInAnyOrderEntriesOf(Map.of(
			CAMUNDA_VARIABLE_HAS_ADMINISTRATOR_AND_APPLICANT, false,
			CAMUNDA_VARIABLE_PHASE_ACTION, PHASE_ACTION_CANCEL,
			CAMUNDA_VARIABLE_PHASE_STATUS, PHASE_STATUS_CANCELED));
	}

	@Test
	void executeThrowsException() {
		// Arrange
		final var problem = Problem.valueOf(Status.I_AM_A_TEAPOT, "Big and stout");
		when(errandMock.getStakeholders()).thenReturn(List.of(
			new Stakeholder().roles(List.of("APPLICANT")),
			new Stakeholder().roles(List.of("ADMINISTRATOR"))));

		when(errandMock.getExtraParameters()).thenReturn(List.of(new ExtraParameter(CASEDATA_KEY_DISPLAY_PHASE).addValuesItem(DISPLAY_PHASE)));

		doThrow(problem).when(externalTaskServiceMock).complete(any(), any());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Assert and verify
		verify(camundaClientMock).setProcessInstanceVariable(PROCESS_INSTANCE_ID, CAMUNDA_VARIABLE_UPDATE_AVAILABLE, FALSE);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_REQUEST_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(errandMock, times(2)).getStakeholders();
		verify(errandMock).getExtraParameters();
		verify(externalTaskServiceMock).complete(eq(externalTaskMock), variablesCaptor.capture());

		assertThat(variablesCaptor.getValue()).containsExactlyEntriesOf(
			Map.of(CAMUNDA_VARIABLE_HAS_ADMINISTRATOR_AND_APPLICANT, true));

		// Verify failure handling
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, problem.getMessage());
		verifyNoMoreInteractions(camundaClientMock, caseDataClientMock, errandMock, externalTaskMock, externalTaskServiceMock);
		verifyNoMoreInteractions(externalTaskServiceMock, camundaClientMock, caseDataClientMock);
	}

	static Stream<List<Stakeholder>> stakeholderLists() {
		return Stream.of(
			List.of(new Stakeholder().roles(List.of("ADMINISTRATOR")),
				new Stakeholder().roles(List.of("USER"))),
			List.of(new Stakeholder().roles(List.of("APPLICANT", "ADMINISTRATOR"))),
			List.of(new Stakeholder().roles(List.of("USER"))),
			List.of(new Stakeholder().roles(emptyList())));
	}
}

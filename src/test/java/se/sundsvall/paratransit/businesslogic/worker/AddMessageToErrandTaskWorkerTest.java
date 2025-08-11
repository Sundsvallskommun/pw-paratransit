package se.sundsvall.paratransit.businesslogic.worker;

import static generated.se.sundsvall.casedata.MessageRequest.DirectionEnum.OUTBOUND;
import static java.time.OffsetDateTime.now;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_CASE_NUMBER;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_MESSAGE_ID;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_NAMESPACE;
import static se.sundsvall.paratransit.Constants.CAMUNDA_VARIABLE_REQUEST_ID;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.MessageAttachment;
import generated.se.sundsvall.casedata.MessageRequest;
import generated.se.sundsvall.templating.RenderResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Component;
import se.sundsvall.paratransit.businesslogic.handler.FailureHandler;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.casedata.CaseDataClient;
import se.sundsvall.paratransit.service.MessagingService;
import se.sundsvall.paratransit.util.DenialTextProperties;
import se.sundsvall.paratransit.util.TextProvider;

@ExtendWith(MockitoExtension.class)
class AddMessageToErrandTaskWorkerTest {

	private static final String REQUEST_ID = "RequestId";
	private static final long ERRAND_ID = 123L;
	private static final String MUNICIPALITY_ID = "2281";
	private static final String NAMESPACE = "SBK_PARKING_PERMIT";

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
	private MessagingService messagingServiceMock;

	@Mock
	private TextProvider textProviderMock;

	@Mock
	private Errand errandMock;

	@InjectMocks
	private AddMessageToErrandTaskWorker worker;

	@Captor
	private ArgumentCaptor<MessageRequest> messageRequestCaptor;

	@Test
	void verifyAnnotations() {
		assertThat(worker.getClass()).hasAnnotations(Component.class, ExternalTaskSubscription.class);
		assertThat(worker.getClass().getAnnotation(ExternalTaskSubscription.class).value()).isEqualTo("AddMessageToErrandTask");
	}

	@Test
	void execute() {
		// Setup
		final var externalCaseID = "externalCaseId";
		final var filename = "filename";
		final var subject = "subject";
		final var plainBody = "plainBody";
		final var templateId = "templateId";
		final var denialTextProperties = new DenialTextProperties();
		denialTextProperties.setTemplateId(templateId);
		denialTextProperties.setFilename(filename);
		denialTextProperties.setSubject(subject);
		denialTextProperties.setPlainBody(plainBody);

		final var messageId = UUID.randomUUID().toString();

		// Mock
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(textProviderMock.getDenialTexts(MUNICIPALITY_ID)).thenReturn(denialTextProperties);
		when(messagingServiceMock.renderPdfDecision(MUNICIPALITY_ID, errandMock, templateId)).thenReturn(new RenderResponse());
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MESSAGE_ID)).thenReturn(messageId);
		when(errandMock.getExternalCaseId()).thenReturn(externalCaseID);

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(messagingServiceMock).renderPdfDecision(MUNICIPALITY_ID, errandMock, templateId);
		verify(textProviderMock, times(4)).getDenialTexts(MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MESSAGE_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(caseDataClientMock).addMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), messageRequestCaptor.capture());
		verify(externalTaskServiceMock).complete(externalTaskMock);
		verifyNoInteractions(failureHandlerMock, camundaClientMock);

		assertThat(messageRequestCaptor.getValue().getDirection()).isEqualTo(OUTBOUND);
		assertThat(messageRequestCaptor.getValue().getMessageId()).isEqualTo(messageId);
		assertThat(messageRequestCaptor.getValue().getSubject()).isEqualTo(subject);
		assertThat(messageRequestCaptor.getValue().getMessage()).isEqualTo(plainBody);
		assertThat(messageRequestCaptor.getValue().getExternalCaseId()).isEqualTo(externalCaseID);
		assertThat(OffsetDateTime.parse(messageRequestCaptor.getValue().getSent())).isCloseTo(now(), within(2, SECONDS));
		assertThat(messageRequestCaptor.getValue().getAttachments()).hasSize(1).extracting(MessageAttachment::getName).containsExactly(filename);
	}

	@Test
	void executeThrowsException() {
		// Setup
		final var templateId = "templateId";
		final var denialTextProperties = new DenialTextProperties();
		denialTextProperties.setTemplateId(templateId);
		// Mock to simulate not finding id of sent message as a process variable
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_REQUEST_ID)).thenReturn(REQUEST_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_CASE_NUMBER)).thenReturn(ERRAND_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID)).thenReturn(MUNICIPALITY_ID);
		when(externalTaskMock.getVariable(CAMUNDA_VARIABLE_NAMESPACE)).thenReturn(NAMESPACE);
		when(caseDataClientMock.getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID)).thenReturn(errandMock);
		when(textProviderMock.getDenialTexts(MUNICIPALITY_ID)).thenReturn(denialTextProperties);
		when(messagingServiceMock.renderPdfDecision(MUNICIPALITY_ID, errandMock, templateId)).thenReturn(new RenderResponse());

		// Act
		worker.execute(externalTaskMock, externalTaskServiceMock);

		// Verify and assert
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_CASE_NUMBER);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_NAMESPACE);
		verify(caseDataClientMock).getErrandById(MUNICIPALITY_ID, NAMESPACE, ERRAND_ID);
		verify(messagingServiceMock).renderPdfDecision(MUNICIPALITY_ID, errandMock, templateId);
		verify(textProviderMock, times(2)).getDenialTexts(MUNICIPALITY_ID);
		verify(externalTaskMock).getVariable(CAMUNDA_VARIABLE_MESSAGE_ID);
		verify(failureHandlerMock).handleException(externalTaskServiceMock, externalTaskMock, "Internal Server Error: Id of sent message could not be retreived from stored process variables");
		verify(externalTaskMock).getId();
		verify(externalTaskMock).getBusinessKey();
		verify(caseDataClientMock, never()).addMessage(eq(MUNICIPALITY_ID), eq(NAMESPACE), eq(ERRAND_ID), any());
		verify(externalTaskServiceMock, never()).complete(any());
		verify(externalTaskServiceMock, never()).complete(any(), any());
		verifyNoInteractions(camundaClientMock);
	}
}

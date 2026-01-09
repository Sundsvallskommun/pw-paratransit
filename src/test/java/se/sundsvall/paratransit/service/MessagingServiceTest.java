package se.sundsvall.paratransit.service;

import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static se.sundsvall.paratransit.Constants.ROLE_APPLICANT;
import static se.sundsvall.paratransit.Constants.ROLE_REPORTER;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.WebMessageRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.paratransit.integration.messaging.MessagingClient;
import se.sundsvall.paratransit.integration.messaging.mapper.MessagingMapper;

@ExtendWith(MockitoExtension.class)
class MessagingServiceTest {

	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private MessagingClient messagingClientMock;

	@Mock
	private MessagingMapper messagingMapperMock;

	@InjectMocks
	private MessagingService messagingService;

	@Test
	void sendMessageSimplifiedServiceWithExternalCaseIdPresent() {

		// Arrange
		final var errand = createErrand(true);
		final var webMessageRequest = new WebMessageRequest();
		final var messageResult = new MessageResult().messageId(UUID.randomUUID());

		when(messagingMapperMock.toWebMessageRequestSimplifiedService(any(), any(), eq(MUNICIPALITY_ID))).thenReturn(webMessageRequest);
		when(messagingClientMock.sendWebMessage(eq(MUNICIPALITY_ID), any())).thenReturn(messageResult);

		// Act
		final var uuid = messagingService.sendMessageSimplifiedService(MUNICIPALITY_ID, errand);

		// Assert
		assertThat(uuid).isEqualTo(Optional.of(messageResult.getMessageId()));
		verify(messagingMapperMock).toWebMessageRequestSimplifiedService(any(), any(), eq(MUNICIPALITY_ID));
		verify(messagingClientMock).sendWebMessage(MUNICIPALITY_ID, webMessageRequest);
		verifyNoMoreInteractions(messagingClientMock, messagingMapperMock);
	}

	@Test
	void sendMessageSimplifiedServiceWithExternalCaseIdNotPresent() {

		// Arrange
		final var errand = createErrand(false);

		// Act
		final var uuid = messagingService.sendMessageSimplifiedService(MUNICIPALITY_ID, errand);

		// Assert
		assertThat(uuid).isEmpty();
		verifyNoInteractions(messagingClientMock, messagingMapperMock);
	}

	@Test
	void noMessageIdReturnedFromMessaging() {

		// Arrange
		final var errand = createErrand(true);
		final var webMessageRequest = new WebMessageRequest();
		final var messageResult = new MessageResult();

		when(messagingMapperMock.toWebMessageRequestSimplifiedService(any(), any(), eq(MUNICIPALITY_ID))).thenReturn(webMessageRequest);
		when(messagingClientMock.sendWebMessage(eq(MUNICIPALITY_ID), any())).thenReturn(messageResult);

		// Act
		final var exception = assertThrows(ThrowableProblem.class, () -> messagingService.sendMessageSimplifiedService(MUNICIPALITY_ID, errand));

		// Assert
		assertThat(exception.getStatus().getStatusCode()).isEqualTo(BAD_GATEWAY.getStatusCode());
		assertThat(exception.getStatus().getReasonPhrase()).isEqualTo(BAD_GATEWAY.getReasonPhrase());
		assertThat(exception.getMessage()).isEqualTo("Bad Gateway: No message id received from messaging service");
		verify(messagingMapperMock).toWebMessageRequestSimplifiedService(any(), any(), eq(MUNICIPALITY_ID));
		verify(messagingClientMock).sendWebMessage(MUNICIPALITY_ID, webMessageRequest);
		verifyNoMoreInteractions(messagingClientMock, messagingMapperMock);
	}

	private static Errand createErrand(boolean withExternalCaseId) {
		return new Errand()
			.externalCaseId(withExternalCaseId ? "1234" : null)
			.stakeholders(List.of(createStakeholder(ROLE_APPLICANT), createStakeholder(ROLE_REPORTER)));
	}

	public static Stakeholder createStakeholder(String role) {
		return new Stakeholder()
			.type(PERSON)
			.personId("d7af5f83-166a-468b-ab86-da8ca30ea97c")
			.roles(List.of(role));
	}
}

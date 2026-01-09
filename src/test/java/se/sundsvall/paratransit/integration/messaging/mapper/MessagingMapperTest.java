package se.sundsvall.paratransit.integration.messaging.mapper;

import static generated.se.sundsvall.messaging.WebMessageRequest.OepInstanceEnum.EXTERNAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static se.sundsvall.paratransit.Constants.MESSAGING_KEY_FLOW_INSTANCE_ID;

import generated.se.sundsvall.messaging.ExternalReference;
import generated.se.sundsvall.messaging.WebMessageParty;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.paratransit.util.SimplifiedServiceTextProperties;
import se.sundsvall.paratransit.util.TextProvider;

@ExtendWith(MockitoExtension.class)
class MessagingMapperTest {

	private static final UUID PARTY_ID = UUID.randomUUID();
	private static final String MESSAGE = "message";
	private static final String MUNICIPALITY_ID = "2281";

	@Mock
	private SimplifiedServiceTextProperties simplifiedServiceTextPropertiesMock;

	@Mock
	private TextProvider textProviderMock;

	@InjectMocks
	private MessagingMapper messagingMapper;

	@Test
	void toWebMessageRequestSimplifiedService() {
		final var externalCaseId = "externalCaseId";

		when(textProviderMock.getSimplifiedServiceTexts(MUNICIPALITY_ID)).thenReturn(simplifiedServiceTextPropertiesMock);
		when(simplifiedServiceTextPropertiesMock.getMessage()).thenReturn(MESSAGE);

		final var request = messagingMapper.toWebMessageRequestSimplifiedService(PARTY_ID.toString(), externalCaseId, MUNICIPALITY_ID);

		assertThat(request.getParty()).isNotNull().extracting(WebMessageParty::getPartyId, WebMessageParty::getExternalReferences).containsExactly(
			PARTY_ID,
			List.of(new ExternalReference().key(MESSAGING_KEY_FLOW_INSTANCE_ID).value(externalCaseId)));
		assertThat(request.getOepInstance()).isEqualTo(EXTERNAL);
		assertThat(request.getMessage()).isEqualTo(MESSAGE);
		assertThat(request.getAttachments()).isEmpty();

		verify(textProviderMock).getSimplifiedServiceTexts(MUNICIPALITY_ID);
		verify(simplifiedServiceTextPropertiesMock).getMessage();
		verifyNoMoreInteractions(simplifiedServiceTextPropertiesMock);
	}
}

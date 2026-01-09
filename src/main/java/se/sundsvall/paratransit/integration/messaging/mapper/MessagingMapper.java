package se.sundsvall.paratransit.integration.messaging.mapper;

import static generated.se.sundsvall.messaging.WebMessageRequest.OepInstanceEnum.EXTERNAL;
import static se.sundsvall.paratransit.Constants.MESSAGING_KEY_FLOW_INSTANCE_ID;

import generated.se.sundsvall.messaging.ExternalReference;
import generated.se.sundsvall.messaging.WebMessageParty;
import generated.se.sundsvall.messaging.WebMessageRequest;
import java.util.UUID;
import org.springframework.stereotype.Service;
import se.sundsvall.paratransit.util.TextProvider;

@Service
public class MessagingMapper {

	private final TextProvider textProvider;

	MessagingMapper(final TextProvider textProvider) {
		this.textProvider = textProvider;
	}

	public WebMessageRequest toWebMessageRequestSimplifiedService(final String partyId, final String externalCaseId, final String municipalityId) {
		return new WebMessageRequest()
			.message(textProvider.getSimplifiedServiceTexts(municipalityId).getMessage())
			.oepInstance(EXTERNAL)
			.party(new WebMessageParty()
				.partyId(UUID.fromString(partyId))
				.addExternalReferencesItem(new ExternalReference().key(MESSAGING_KEY_FLOW_INSTANCE_ID).value(externalCaseId)));
	}
}

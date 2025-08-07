package se.sundsvall.paratransit.service;

import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static se.sundsvall.paratransit.Constants.ROLE_APPLICANT;
import static se.sundsvall.paratransit.integration.templating.mapper.TemplatingMapper.toRenderRequestWhenNotMemberOfMunicipality;
import static se.sundsvall.paratransit.util.ErrandUtil.getStakeholder;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.templating.RenderResponse;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.paratransit.integration.messaging.MessagingClient;
import se.sundsvall.paratransit.integration.messaging.mapper.MessagingMapper;
import se.sundsvall.paratransit.integration.templating.TemplatingClient;

@Service
public class MessagingService {

	private final MessagingClient messagingClient;

	private final TemplatingClient templatingClient;

	private final MessagingMapper messagingMapper;

	MessagingService(final MessagingClient messagingClient, final TemplatingClient templatingClient, final MessagingMapper messagingMapper) {
		this.messagingClient = messagingClient;
		this.templatingClient = templatingClient;
		this.messagingMapper = messagingMapper;
	}

	public RenderResponse renderPdfDecision(final String municipalityId, final Errand errand) {

		return templatingClient.renderPdf(municipalityId, toRenderRequestWhenNotMemberOfMunicipality(errand));
	}

	public UUID sendMessageToNonCitizen(final String municipalityId, final Errand errand, final RenderResponse pdf) {
		final var partyId = getStakeholder(errand, PERSON, ROLE_APPLICANT).getPersonId();

		if (isNotEmpty(errand.getExternalCaseId())) {
			final var messageResult = messagingClient.sendWebMessage(municipalityId, messagingMapper.toWebMessageRequestDenial(pdf, partyId, errand.getExternalCaseId(), municipalityId));
			return extractId(List.of(messageResult));
		}
		final var messageResult = messagingClient.sendLetter(municipalityId, messagingMapper.toLetterRequestDenial(pdf, partyId, municipalityId));
		return extractId(messageResult.getMessages());
	}

	public UUID sendDenialDecisionMessage(final String municipalityId, final Errand errand, final RenderResponse pdf) {
		final var partyId = getStakeholder(errand, PERSON, ROLE_APPLICANT).getPersonId();

		final var messageResult = messagingClient.sendLetter(municipalityId, messagingMapper.toLetterRequestDenial(pdf, partyId, municipalityId));

		return extractId(messageResult.getMessages());
	}

	public UUID sendMessageSimplifiedService(final String municipalityId, final Errand errand) {
		final var partyId = getStakeholder(errand, PERSON, ROLE_APPLICANT).getPersonId();

		if (isNotEmpty(errand.getExternalCaseId())) {
			final var messageResult = messagingClient.sendWebMessage(municipalityId, messagingMapper.toWebMessageRequestSimplifiedService(partyId, errand.getExternalCaseId(),
				municipalityId));
			return extractId(List.of(messageResult));
		}
		final var messageResult = messagingClient.sendLetter(municipalityId, messagingMapper.toLetterRequestSimplifiedService(partyId, municipalityId));

		return extractId(messageResult.getMessages());
	}

	private UUID extractId(final List<MessageResult> messageResults) {
		return ofNullable(messageResults).orElse(emptyList()).stream()
			.map(MessageResult::getMessageId)
			.filter(Objects::nonNull)
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "No message id received from messaging service"));
	}
}

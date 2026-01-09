package se.sundsvall.paratransit.service;

import static generated.se.sundsvall.casedata.Stakeholder.TypeEnum.PERSON;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.zalando.problem.Status.BAD_GATEWAY;
import static se.sundsvall.paratransit.Constants.ROLE_APPLICANT;
import static se.sundsvall.paratransit.util.ErrandUtil.getStakeholder;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.messaging.MessageResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.paratransit.integration.messaging.MessagingClient;
import se.sundsvall.paratransit.integration.messaging.mapper.MessagingMapper;

@Service
public class MessagingService {

	private final MessagingClient messagingClient;

	private final MessagingMapper messagingMapper;

	MessagingService(final MessagingClient messagingClient, final MessagingMapper messagingMapper) {
		this.messagingClient = messagingClient;
		this.messagingMapper = messagingMapper;
	}

	public Optional<UUID> sendMessageSimplifiedService(final String municipalityId, final Errand errand) {
		final var partyId = getStakeholder(errand, PERSON, ROLE_APPLICANT).getPersonId();

		if (isNotEmpty(errand.getExternalCaseId())) {
			final var messageResult = messagingClient.sendWebMessage(municipalityId, messagingMapper.toWebMessageRequestSimplifiedService(partyId, errand.getExternalCaseId(),
				municipalityId));
			return Optional.of(extractId(List.of(messageResult)));
		}

		return Optional.empty();
	}

	private UUID extractId(final List<MessageResult> messageResults) {
		return ofNullable(messageResults).orElse(emptyList()).stream()
			.map(MessageResult::getMessageId)
			.filter(Objects::nonNull)
			.findFirst()
			.orElseThrow(() -> Problem.valueOf(BAD_GATEWAY, "No message id received from messaging service"));
	}
}

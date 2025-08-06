package se.sundsvall.paratransit.integration.casedata.mapper;

import static java.time.OffsetDateTime.now;
import static java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_STATUS;

import generated.se.sundsvall.casedata.Attachment;
import generated.se.sundsvall.casedata.Decision;
import generated.se.sundsvall.casedata.Decision.DecisionOutcomeEnum;
import generated.se.sundsvall.casedata.Decision.DecisionTypeEnum;
import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.Law;
import generated.se.sundsvall.casedata.MessageAttachment;
import generated.se.sundsvall.casedata.MessageRequest;
import generated.se.sundsvall.casedata.MessageRequest.DirectionEnum;
import generated.se.sundsvall.casedata.PatchErrand;
import generated.se.sundsvall.casedata.Stakeholder;
import generated.se.sundsvall.casedata.Stakeholder.TypeEnum;
import generated.se.sundsvall.casedata.Status;
import generated.se.sundsvall.templating.RenderResponse;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CaseDataMapper {

	private CaseDataMapper() {}

	public static PatchErrand toPatchErrand(final Errand errand, final String phase, final String displayPhase, final String phaseStatus, final String phaseAction) {
		var patchErrand = toPatchErrand(errand, phase, phaseStatus, phaseAction);
		return patchErrand.extraParameters(patchErrand.getExtraParameters().stream()
			.filter(e -> !CASEDATA_KEY_DISPLAY_PHASE.equals(e.getKey()))
			.collect(Collectors.toCollection(ArrayList::new)))
			.addExtraParametersItem((new ExtraParameter(CASEDATA_KEY_DISPLAY_PHASE).values(displayPhase == null ? emptyList() : List.of(displayPhase))));
	}

	public static PatchErrand toPatchErrand(final Errand errand, final String phase, final String phaseStatus, final String phaseAction) {
		return new PatchErrand()
			.externalCaseId(errand.getExternalCaseId())
			.phase(phase)
			.facilities(null)
			.extraParameters(ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
				.filter(e -> !CASEDATA_KEY_PHASE_STATUS.equals(e.getKey()))
				.filter(e -> !CASEDATA_KEY_PHASE_ACTION.equals(e.getKey()))
				.collect(Collectors.toCollection(ArrayList::new)))
			.addExtraParametersItem(new ExtraParameter(CASEDATA_KEY_PHASE_STATUS).values(phaseStatus == null ? emptyList() : List.of(phaseStatus)))
			.addExtraParametersItem(new ExtraParameter(CASEDATA_KEY_PHASE_ACTION).values(phaseAction == null ? emptyList() : List.of(phaseAction)));
	}

	public static PatchErrand toPatchErrand(final Errand errand, final String externalCaseId, final String phaseAction) {
		return new PatchErrand()
			.externalCaseId(externalCaseId)
			.facilities(null)
			.extraParameters(ofNullable(errand.getExtraParameters()).orElse(emptyList()).stream()
				.filter(e -> !CASEDATA_KEY_PHASE_ACTION.equals(e.getKey()))
				.collect(Collectors.toCollection(ArrayList::new)))
			.addExtraParametersItem(new ExtraParameter(CASEDATA_KEY_PHASE_ACTION).addValuesItem(phaseAction));
	}

	public static Stakeholder toStakeholder(final String role, final TypeEnum type, final String firstName, final String lastName) {
		final var bean = new Stakeholder()
			.firstName(firstName)
			.lastName(lastName)
			.type(type);

		ofNullable(role).ifPresent(bean::addRolesItem);

		return bean;
	}

	public static Decision toDecision(final DecisionTypeEnum decisionType, final DecisionOutcomeEnum decisionOutcome, final String description) {
		return new Decision()
			.created(getNow())
			.decisionType(decisionType)
			.decisionOutcome(decisionOutcome)
			.description(description);
	}

	public static Law toLaw(final String heading, final String sfs, final String chapter, final String article) {
		return new Law()
			.heading(heading)
			.sfs(sfs)
			.chapter(chapter)
			.article(article);
	}

	public static Attachment toAttachment(final String category, final String name, final String extension, final String mimeType, final RenderResponse renderedContent) {
		final var bean = new Attachment()
			.category(category)
			.name(name)
			.extension(extension)
			.mimeType(mimeType);

		ofNullable(renderedContent)
			.map(RenderResponse::getOutput)
			.ifPresent(bean::setFile);

		return bean;
	}

	public static MessageAttachment toMessageAttachment(final String fileName, final String contentType, final RenderResponse renderedContent) {
		final var bean = new MessageAttachment()
			.name(fileName)
			.contentType(contentType);

		ofNullable(renderedContent)
			.map(RenderResponse::getOutput)
			.ifPresent(bean::setContent);

		return bean;
	}

	public static MessageRequest toMessageRequest(final String messageId, final String subject, final String message, final Errand errand, final DirectionEnum direction, final String username, final MessageAttachment attachment) {
		final var bean = new MessageRequest()
			.messageId(messageId)
			.direction(direction)
			.message(message)
			.subject(subject)
			.sent(ISO_OFFSET_DATE_TIME.format(getNow()))
			.username(username);

		ofNullable(errand).ifPresent(theErrand -> bean
			.externalCaseId(theErrand.getExternalCaseId()));

		ofNullable(attachment)
			.ifPresent(bean::addAttachmentsItem);

		return bean;
	}

	public static Status toStatus(final String statusType, final String description) {
		return new Status()
			.statusType(statusType)
			.created(getNow())
			.description(description);
	}

	private static OffsetDateTime getNow() {
		return now(ZoneId.systemDefault());
	}
}

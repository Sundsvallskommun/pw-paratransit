package se.sundsvall.paratransit.integration.casedata.mapper;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.PatchErrand;
import generated.se.sundsvall.casedata.Status;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static java.time.OffsetDateTime.now;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_STATUS;

public class CaseDataMapper {

	private CaseDataMapper() {}

	public static PatchErrand toPatchErrand(final Errand errand, final String phase) {
		return new PatchErrand()
			.externalCaseId(errand.getExternalCaseId())
			.phase(phase)
			.extraParameters(null)
			.facilities(null);
	}

	public static Status toStatus(final String statusType, final String description) {
		return new Status()
			.statusType(statusType)
			.created(getNow())
			.description(description);
	}

	public static List<ExtraParameter> toExtraParameters(final String displayPhase, final String phaseStatus, final String phaseAction) {
		final List<ExtraParameter> extraParameters = toExtraParameters(phaseStatus, phaseAction);
		if (displayPhase != null) {
			extraParameters.add(new ExtraParameter(CASEDATA_KEY_DISPLAY_PHASE).values(List.of(displayPhase)));
		}
		return extraParameters;
	}

	public static List<ExtraParameter> toExtraParameters(final String phaseStatus, final String phaseAction) {
		final List<ExtraParameter> extraParameters = new ArrayList<>();
		if (phaseStatus != null) {
			extraParameters.add(new ExtraParameter(CASEDATA_KEY_PHASE_STATUS).values(List.of(phaseStatus)));
		}
		if (phaseAction != null) {
			extraParameters.add(new ExtraParameter(CASEDATA_KEY_PHASE_ACTION).values(List.of(phaseAction)));
		}
		return extraParameters;
	}

	private static OffsetDateTime getNow() {
		return now(ZoneId.systemDefault());
	}
}

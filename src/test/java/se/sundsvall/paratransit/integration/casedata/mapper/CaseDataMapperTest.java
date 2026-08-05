package se.sundsvall.paratransit.integration.casedata.mapper;

import generated.se.sundsvall.casedata.Errand;
import generated.se.sundsvall.casedata.ExtraParameter;
import generated.se.sundsvall.casedata.PatchErrand;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.time.OffsetDateTime.now;
import static java.time.ZoneId.systemDefault;
import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_DISPLAY_PHASE;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_ACTION;
import static se.sundsvall.paratransit.Constants.CASEDATA_KEY_PHASE_STATUS;

@ExtendWith(MockitoExtension.class)
class CaseDataMapperTest {

	@Test
	void toStatusFromTypeAndDescription() {
		final var statusType = "statusType";
		final var description = "description";
		final var bean = CaseDataMapper.toStatus(statusType, description);

		assertThat(bean.getStatusType()).isEqualTo(statusType);
		assertThat(bean.getDescription()).isEqualTo(description);
		assertThat(bean.getCreated()).isCloseTo(now(systemDefault()), within(2, SECONDS));
	}

	@Test
	void toPatchErrand() {
		final var externalCaseId = "externalCaseId";
		final var errand = new Errand().externalCaseId(externalCaseId);
		final var phase = "phase";

		final var bean = CaseDataMapper.toPatchErrand(errand, phase);

		assertThat(bean).isNotNull()
			.extracting(
				PatchErrand::getExternalCaseId,
				PatchErrand::getPhase)
			.containsExactly(
				externalCaseId,
				phase);
	}

	@Test
	void toExtraParameters() {
		final var displayPhase = "displayPhase";
		final var phaseStatus = "phaseStatus";
		final var phaseAction = "phaseAction";
		final var extraParameters = CaseDataMapper.toExtraParameters(displayPhase, phaseStatus, phaseAction);

		assertThat(extraParameters).extracting(ExtraParameter::getKey, ExtraParameter::getValues).containsExactlyInAnyOrder(
			org.assertj.core.api.Assertions.tuple(CASEDATA_KEY_DISPLAY_PHASE, List.of(displayPhase)),
			org.assertj.core.api.Assertions.tuple(CASEDATA_KEY_PHASE_STATUS, List.of(phaseStatus)),
			org.assertj.core.api.Assertions.tuple(CASEDATA_KEY_PHASE_ACTION, List.of(phaseAction)));
	}

	@Test
	void toExtraParametersWhenNullValues() {
		final var extraParameters = CaseDataMapper.toExtraParameters(null, null, null);

		assertThat(extraParameters).isEmpty();
	}

	@Test
	void toExtraParametersWithPhaseStatusAndPhaseAction() {
		final var phaseStatus = "phaseStatus";
		final var phaseAction = "phaseAction";
		final var extraParameters = CaseDataMapper.toExtraParameters(phaseStatus, phaseAction);

		assertThat(extraParameters).extracting(ExtraParameter::getKey, ExtraParameter::getValues).containsExactlyInAnyOrder(
			org.assertj.core.api.Assertions.tuple(CASEDATA_KEY_PHASE_STATUS, List.of(phaseStatus)),
			org.assertj.core.api.Assertions.tuple(CASEDATA_KEY_PHASE_ACTION, List.of(phaseAction)));
	}

	@Test
	void toExtraParametersWithPhaseStatusAndPhaseActionWhenNullValues() {
		final var extraParameters = CaseDataMapper.toExtraParameters(null, null);

		assertThat(extraParameters).isEmpty();
	}

}

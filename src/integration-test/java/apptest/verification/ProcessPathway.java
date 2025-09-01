package apptest.verification;

import static org.assertj.core.api.Assertions.tuple;

public class ProcessPathway {

	public static Tuples actualizationPathway() {
		return Tuples.create()
			.with(tuple("Actualization", "actualization_phase"))
			.with(tuple("Start actualization phase", "start_actualization_phase"))
			.with(tuple("Update phase", "external_task_actualization_update_phase"))
			.with(tuple("Verify resident of municipality", "external_task_verify_resident_of_municipality_task"))
			.with(tuple("Is citizen of municipality", "gateway_actualization_is_citizen_of_municipality"))
			.with(tuple("Verify that reporter stakeholder exists", "external_task_actualization_verify_reporter_stakeholder_exists_task"))
			.with(tuple("Is stakeholder with role REPORTER assigned", "gateway_actualization_stakeholder_reporter_is_assigned"))
			.with(tuple("Update displayPhase", "external_task_actualization_update_display_phase"))
			.with(tuple("Update errand status", "external_task_actualization_update_errand_status_to_under_review"))
			.with(tuple("Check phase action", "external_task_actualization_check_phase_action_task"))
			.with(tuple("Is phase action complete?", "gateway_actualization_is_phase_action_complete"))
			.with(tuple("End actualization phase", "end_actualization_phase"));
	}

	public static Tuples investigationPathway() {
		return Tuples.create()
			.with(tuple("Investigation", "investigation_phase"))
			.with(tuple("Start investigation phase", "start_investigation_phase"))
			.with(tuple("Update phase", "external_task_investigation_update_phase"))
			.with(tuple("Update errand status", "external_task_investigation_update_errand_status"))
			.with(tuple("Check phase action", "external_task_investigation_check_phase_action_task"))
			.with(tuple("Is phase action complete?", "gateway_investigation_is_phase_action_complete"))
			.with(tuple("End investigation phase", "end_investigation_phase"));
	}

	public static Tuples decisionPathway() {
		return Tuples.create()
			.with(tuple("Decision", "decision_phase"))
			.with(tuple("Start decision phase", "start_decision_phase"))
			.with(tuple("Update phase on errand", "external_task_decision_update_phase"))
			.with(tuple("Update errand status", "external_task_decision_update_errand_status"))
			.with(tuple("Check if decision is made", "external_task_check_decision_task"))
			.with(tuple("Gateway is decision final", "gateway_is_decision_final"))
			.with(tuple("End decision phase", "end_decision_phase"));
	}

	public static Tuples handlingPathway() {
		return Tuples.create()
			.with(tuple("Handling", "call_activity_handling"))
			.with(tuple("Start handling phase", "start_handling_phase"))
			.with(tuple("End handling phase", "end_handling_phase"));
	}

	public static Tuples executionPathway() {
		return Tuples.create()
			.with(tuple("Execution", "call_activity_execution"))
			.with(tuple("Start execution phase", "start_execution_phase"))
			.with(tuple("Update phase", "external_task_execution_update_phase"))
			//Added delay to send control message to make it happen after the asset is created
			.with(tuple("Wait to send message", "timer_wait_to_send_message"))
			.with(tuple("Send simplified service message", "external_task_execution_send_message_task"))
			.with(tuple("End execution phase", "end_execution_phase"));
	}

	public static Tuples followUpPathway() {
		return Tuples.create()
			.with(tuple("Follow up", "call_activity_follow_up"))
			.with(tuple("Start follow up phase", "start_follow_up_phase"))
			.with(tuple("Update phase", "external_task_follow_up_update_phase"))
			.with(tuple("Check phase action", "external_task_followup_check_phase_action"))
			.with(tuple("Is phase action complete?", "gateway_followup_is_phase_action_complete"))
			.with(tuple("Clean up notes", "external_task_follow_up_clean_up_notes"))
			.with(tuple("Update errand status", "external_task_follow_up_update_status"))
			.with(tuple("Update phase action", "external_task_follow_up_update_phase_action"))
			.with(tuple("End follow up phase", "end_follow_up_phase"));
	}

	public static Tuples canceledPathway() {
		return Tuples.create()
			.with(tuple("Canceled", "canceled_phase"))
			.with(tuple("Start canceled phase", "start_canceled_phase"))
			.with(tuple("Update phase", "external_task_canceled_update_phase"))
			.with(tuple("Update errand status", "external_task_canceled_update_errand_status"))
			.with(tuple("Clean up notes", "external_task_canceled_clean_up_notes"))
			.with(tuple("End canceled phase", "end_canceled_phase"));
	}

	public static Tuples denialPathway() {
		return Tuples.create()
			.with(tuple("Start automatic denial phase", "start_automatic_denial_phase"))
			.with(tuple("Update phase on errand", "external_task_update_errand_phase"))
			.with(tuple("Add decision for denial to errand", "external_task_add_denial_decision"))
			.with(tuple("Update errand status", "external_task_update_errand_status"))
			.with(tuple("Send denial decision to applicant", "external_task_send_denial_decision"))
			.with(tuple("Add message to errand", "external_task_add_message"))
			.with(tuple("Wait to send message", "timer_denial_wait_to_send_message"))
			.with(tuple("Send simplified service message", "external_task_send_simplified_service"))
			.with(tuple("End automatic denial phase", "end_automatic_denial_phase"))
			.with(tuple("Automatic denial", "subprocess_automatic_denial"));
	}
}

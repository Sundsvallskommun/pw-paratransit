package se.sundsvall.workflow.service;

import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.workflow.Constants.PROCESS_KEY;
import static se.sundsvall.workflow.Constants.TENANTID_TEMPLATE;
import static se.sundsvall.workflow.Constants.TRUE;
import static se.sundsvall.workflow.Constants.UPDATE_AVAILABLE;

import generated.se.sundsvall.camunda.StartProcessInstanceDto;
import org.springframework.stereotype.Service;
import org.zalando.problem.Problem;
import se.sundsvall.workflow.integration.camunda.CamundaClient;

@Service
public class ProcessService {

	private final CamundaClient camundaClient;

	public ProcessService(CamundaClient camundaClient) {
		this.camundaClient = camundaClient;
	}

	public String startProcess(String businessKey) {
		return camundaClient.startProcessWithTenant(PROCESS_KEY, TENANTID_TEMPLATE, new StartProcessInstanceDto().businessKey(businessKey)).getId();
	}

	public void updateProcess(String processInstanceId) {

		verifyExistingProcessInstance(processInstanceId);

		camundaClient.setProcessInstanceVariable(processInstanceId, UPDATE_AVAILABLE, TRUE);
	}

	private void verifyExistingProcessInstance(String processInstanceId) {
		if (camundaClient.getProcessInstance(processInstanceId).isEmpty()) {
			throw Problem.valueOf(NOT_FOUND, "Process instance with ID '%s' does not exist!".formatted(processInstanceId));
		}
	}
}

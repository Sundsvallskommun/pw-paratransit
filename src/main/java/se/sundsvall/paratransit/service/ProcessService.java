package se.sundsvall.paratransit.service;

import generated.se.sundsvall.camunda.VariableValueDto;
import java.util.Map;
import java.util.Objects;
import org.camunda.bpm.engine.variable.type.ValueType;
import org.springframework.stereotype.Service;
import se.sundsvall.dept44.problem.Problem;
import se.sundsvall.dept44.requestid.RequestId;
import se.sundsvall.paratransit.integration.camunda.CamundaClient;
import se.sundsvall.paratransit.integration.camunda.mapper.CamundaMapper;
import se.sundsvall.paratransit.integration.operaton.OperatonClient;
import se.sundsvall.paratransit.integration.operaton.mapper.OperatonMapper;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static se.sundsvall.paratransit.Constants.PROCESS_KEY;
import static se.sundsvall.paratransit.Constants.PROCESS_VARIABLE_MUNICIPALITY_ID;
import static se.sundsvall.paratransit.Constants.PROCESS_VARIABLE_NAMESPACE;
import static se.sundsvall.paratransit.Constants.PROCESS_VARIABLE_REQUEST_ID;
import static se.sundsvall.paratransit.Constants.PROCESS_VARIABLE_UPDATE_AVAILABLE;
import static se.sundsvall.paratransit.Constants.TENANTID_TEMPLATE;
import static se.sundsvall.paratransit.Constants.TRUE;

@Service
public class ProcessService {

	private final CamundaClient camundaClient;

	private final OperatonClient operatonClient;

	ProcessService(CamundaClient camundaClient, OperatonClient operatonClient) {
		this.camundaClient = camundaClient;
		this.operatonClient = operatonClient;
	}

	public String startProcess(final String municipalityId, final String namespace, final Long caseNumber) {
		// New processes are always created in Operaton.
		return operatonClient.startProcessWithTenant(PROCESS_KEY, TENANTID_TEMPLATE, OperatonMapper.toStartProcessInstanceDto(municipalityId, namespace, caseNumber)).getId();
	}

	public void updateProcess(final String municipalityId, final String namespace, final String processInstanceId) {
		// New processes live in Operaton, older ones still in Camunda. Probe Operaton first and fall back to Camunda.
		if (operatonClient.getProcessInstance(processInstanceId).isPresent() && belongsToOperaton(municipalityId, namespace, processInstanceId)) {
			operatonClient.setProcessInstanceVariables(processInstanceId, operatonUpdateVariables(municipalityId, namespace));
		} else if (camundaClient.getProcessInstance(processInstanceId).isPresent() && belongsToCamunda(municipalityId, namespace, processInstanceId)) {
			camundaClient.setProcessInstanceVariables(processInstanceId, camundaUpdateVariables(municipalityId, namespace));
		} else {
			throw Problem.valueOf(NOT_FOUND, "Process instance with ID '%s' does not exist!".formatted(processInstanceId));
		}
	}

	private generated.se.sundsvall.operaton.PatchVariablesDto operatonUpdateVariables(final String municipalityId, final String namespace) {
		return OperatonMapper.toPatchVariablesDto(Map.of(
			PROCESS_VARIABLE_MUNICIPALITY_ID, OperatonMapper.toVariableValueDto(ValueType.STRING, municipalityId),
			PROCESS_VARIABLE_NAMESPACE, OperatonMapper.toVariableValueDto(ValueType.STRING, namespace),
			PROCESS_VARIABLE_UPDATE_AVAILABLE, OperatonMapper.toVariableValueDto(ValueType.BOOLEAN, true),
			PROCESS_VARIABLE_REQUEST_ID, OperatonMapper.toVariableValueDto(ValueType.STRING, RequestId.get())));
	}

	private generated.se.sundsvall.camunda.PatchVariablesDto camundaUpdateVariables(final String municipalityId, final String namespace) {
		return CamundaMapper.toPatchVariablesDto(Map.of(
			PROCESS_VARIABLE_MUNICIPALITY_ID, CamundaMapper.toVariableValueDto(ValueType.STRING, municipalityId),
			PROCESS_VARIABLE_NAMESPACE, CamundaMapper.toVariableValueDto(ValueType.STRING, namespace),
			PROCESS_VARIABLE_UPDATE_AVAILABLE, TRUE,
			PROCESS_VARIABLE_REQUEST_ID, CamundaMapper.toVariableValueDto(ValueType.STRING, RequestId.get())));
	}

	/**
	 * Verifies that the process instance belongs to the provided municipality and namespace. A process instance owned by
	 * another municipality or namespace is reported as non existing, to avoid disclosing process instances outside of the
	 * callers scope.
	 */
	private boolean belongsToOperaton(final String municipalityId, final String namespace, final String processInstanceId) {
		final var variables = ofNullable(operatonClient.getProcessInstanceVariables(processInstanceId)).orElse(emptyMap());

		return Objects.equals(municipalityId, ofNullable(variables.get(PROCESS_VARIABLE_MUNICIPALITY_ID)).map(generated.se.sundsvall.operaton.VariableValueDto::getValue).map(String::valueOf).orElse(null))
			&& Objects.equals(namespace, ofNullable(variables.get(PROCESS_VARIABLE_NAMESPACE)).map(generated.se.sundsvall.operaton.VariableValueDto::getValue).map(String::valueOf).orElse(null));
	}

	/**
	 * @see #belongsToOperaton(String, String, String)
	 */
	private boolean belongsToCamunda(final String municipalityId, final String namespace, final String processInstanceId) {
		final var variables = ofNullable(camundaClient.getProcessInstanceVariables(processInstanceId)).orElse(emptyMap());

		return Objects.equals(municipalityId, ofNullable(variables.get(PROCESS_VARIABLE_MUNICIPALITY_ID)).map(VariableValueDto::getValue).map(String::valueOf).orElse(null))
			&& Objects.equals(namespace, ofNullable(variables.get(PROCESS_VARIABLE_NAMESPACE)).map(VariableValueDto::getValue).map(String::valueOf).orElse(null));
	}
}

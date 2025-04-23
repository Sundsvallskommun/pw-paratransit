package se.sundsvall.workflow.businesslogic.worker;

import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;
import se.sundsvall.workflow.businesslogic.handler.FailureHandler;

@Component
@ExternalTaskSubscription("myWorker")
public class MyWorker implements ExternalTaskHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MyWorker.class);

	private final FailureHandler failureHandler;

	public MyWorker(FailureHandler failureHandler) {
		this.failureHandler = failureHandler;
	}

	@Override
	public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {
		try {
			LOGGER.info("Execute My Worker");
			if ("throw_exception".equals(externalTask.getBusinessKey())) {
				throw Problem.valueOf(Status.I_AM_A_TEAPOT, "Big and stout");
			}
			LOGGER.info("Finally made it, printing some message for task with id {} and businesskey {}", externalTask.getId(), externalTask.getBusinessKey());

			externalTaskService.complete(externalTask);
		} catch (final Exception exception) {
			LOGGER.error("Exception occurred in execution for task with id {} and businesskey {}", externalTask.getId(), externalTask.getBusinessKey());

			failureHandler.handleException(externalTaskService, externalTask, exception.getMessage());
		}
	}
}

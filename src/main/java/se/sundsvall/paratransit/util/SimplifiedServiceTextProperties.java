package se.sundsvall.paratransit.util;

public class SimplifiedServiceTextProperties {

	private String message;
	private String subject;
	private String htmlBody;
	private String plainBody;
	private String description;
	private String delay;

	public String getMessage() {
		return message;
	}

	public void setMessage(final String message) {
		this.message = message;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(final String subject) {
		this.subject = subject;
	}

	public String getHtmlBody() {
		return htmlBody;
	}

	public void setHtmlBody(final String htmlBody) {
		this.htmlBody = htmlBody;
	}

	public String getPlainBody() {
		return plainBody;
	}

	public void setPlainBody(final String plainBody) {
		this.plainBody = plainBody;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(final String description) {
		this.description = description;
	}

	public String getDelay() {
		return delay;
	}

	public void setDelay(final String delay) {
		this.delay = delay;
	}
}

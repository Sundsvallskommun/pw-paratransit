package se.sundsvall.paratransit.util;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "texts")
public class TextProperties {

	private Map<String, SimplifiedServiceTextProperties> simplifiedServices;

	public Map<String, SimplifiedServiceTextProperties> getSimplifiedServices() {
		return simplifiedServices;
	}

	public void setSimplifiedServices(final Map<String, SimplifiedServiceTextProperties> simplifiedServices) {
		this.simplifiedServices = simplifiedServices;
	}
}

package se.sundsvall.paratransit.util;

import org.springframework.stereotype.Component;

@Component
public class TextProvider {

	private final TextProperties textProperties;

	TextProvider(final TextProperties textProperties) {
		this.textProperties = textProperties;
	}

	public SimplifiedServiceTextProperties getSimplifiedServiceTexts(final String municipalityId) {

		return textProperties.getSimplifiedServices().get(municipalityId);
	}
}

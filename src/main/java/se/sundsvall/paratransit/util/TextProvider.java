package se.sundsvall.paratransit.util;

import org.springframework.stereotype.Component;

@Component
public class TextProvider {

	private final TextProperties textProperties;

	TextProvider(final TextProperties textProperties) {
		this.textProperties = textProperties;
	}

	public CommonTextProperties getCommonTexts(final String municipalityId) {

		return textProperties.getCommons().get(municipalityId);
	}

	public DenialTextProperties getDenialTexts(final String municipalityId) {

		return textProperties.getDenials().get(municipalityId);
	}

	public SimplifiedServiceTextProperties getSimplifiedServiceTexts(final String municipalityId) {

		return textProperties.getSimplifiedServices().get(municipalityId);
	}
}

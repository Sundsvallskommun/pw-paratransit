package se.sundsvall.paratransit.util;

import org.springframework.stereotype.Component;

@Component
public class TextProvider {

	private final TextProperties textProperties;

	TextProvider(final TextProperties textProperties) {
		this.textProperties = textProperties;
	}

	public CommonTextProperties getCommonTexts(String municipalityId) {
		return textProperties.getCommons().get(municipalityId);
	}

	public DenialTextProperties getDenialTexts(String municipalityId) {
		return textProperties.getDenials().get(municipalityId);
	}

	public SimplifiedServiceTextProperties getSimplifiedServiceTexts(String municipalityId) {
		return textProperties.getSimplifiedServices().get(municipalityId);
	}
}

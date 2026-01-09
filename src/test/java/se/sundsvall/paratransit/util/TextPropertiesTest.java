package se.sundsvall.paratransit.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.paratransit.Application;

@SpringBootTest(classes = Application.class, webEnvironment = MOCK)
@ActiveProfiles("junit")
class TextPropertiesTest {

	static final String MUNICIPALITY_ID = "2281";

	// Simplified service text properties
	private static final String SIMPLIFIED_MESSAGE = """
		Kontrollmeddelande för förenklad delgivning

		Vi har nyligen delgivit dig ett beslut via brev. Du får nu ett kontrollmeddelande för att säkerställa att du mottagit informationen.
		När det har gått två veckor från det att beslutet skickades anses du blivit delgiven och du har då tre veckor på dig att överklaga beslutet.
		Om du bara fått kontrollmeddelandet men inte själva delgivningen med beslutet måste du kontakta oss via e-post till
		kontakt@sundsvall.se eller telefon till 060-19 10 00.""";

	@Autowired
	private TextProperties textProperties;

	@Test
	void simplifiedServiceTexts() {
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getMessage()).isEqualTo(SIMPLIFIED_MESSAGE);
		assertThat(textProperties.getSimplifiedServices().get(MUNICIPALITY_ID).getDelay()).isEqualTo("P1D");
	}
}

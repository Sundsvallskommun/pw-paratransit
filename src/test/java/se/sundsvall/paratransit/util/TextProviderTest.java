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
class TextProviderTest {

	static final String MUNICIPALITY_ID = "2281";

	@Autowired
	private TextProvider textProvider;

	@Test
	void checkCorrectAutowiring() {
		assertThat(textProvider.getCommonTexts(MUNICIPALITY_ID)).isNotNull();
		assertThat(textProvider.getDenialTexts(MUNICIPALITY_ID)).isNotNull();
		assertThat(textProvider.getSimplifiedServiceTexts(MUNICIPALITY_ID)).isNotNull();
	}
}

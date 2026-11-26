package cloud.cholewa.media.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

//HAS-515: the timeout of the unlock is a bare number of seconds, never milliseconds, between 1 and 30 s; the address has
//no default and is never blank
class AmplifierPropertiesTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static AmplifierProperties bind(final String address, final String timeout) {
        return new Binder(new MapConfigurationPropertySource(Map.of(
            "media.amplifier.address", address,
            "media.amplifier.timeout", timeout)))
            .bind("media.amplifier", AmplifierProperties.class).get();
    }

    @Test
    void should_bind_a_bare_number_as_seconds() {
        assertThat(bind("amplifier", "5").timeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "30", "PT5S"})
    void should_take_the_bounds(final String timeout) {
        assertThat(validator.validate(bind("amplifier", timeout))).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "31", "500ms"})
    void should_refuse_a_timeout_outside_the_bounds(final String timeout) {
        assertThat(validator.validate(bind("amplifier", timeout))).isNotEmpty();
    }

    @Test
    void should_refuse_a_blank_address() {
        assertThat(validator.validate(bind(" ", "5"))).isNotEmpty();
    }
}

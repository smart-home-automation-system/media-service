package cloud.cholewa.media.error;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

//The names of the causes are the codes of the answers - wire contract for the dashboard (HAS-517), which words each of
//them: renaming one is a breaking change, made together with the dashboard
class AmplifierErrorTest {

    @Test
    void should_keep_the_names_of_the_codes() {
        assertThat(Arrays.stream(AmplifierError.values()).map(Enum::name)).containsExactly(
            "AMPLIFIER_REFUSED", "AMPLIFIER_UNAVAILABLE", "AMPLIFIER_TIMEOUT", "UNLOCK_IN_PROGRESS", "UNKNOWN_MEMBER",
            "NOT_PERMITTED", "REGISTRY_UNAVAILABLE");
    }
}

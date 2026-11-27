package cloud.cholewa.media.amplifier;

import cloud.cholewa.media.config.ExceptionHandlerConfig;
import cloud.cholewa.media.error.AmplifierException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

//HAS-515: POST /amplifier/unlock - 202, and every refusal with its status and its code for the dashboard
@WebFluxTest(AmplifierController.class)
@Import(ExceptionHandlerConfig.class)
@ActiveProfiles("test")
class AmplifierControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private AmplifierService amplifierService;

    @Test
    void should_accept_an_unlock() {
        when(amplifierService.unlock("dashboard", "Anna")).thenReturn(Mono.empty());

        webTestClient.post().uri("/amplifier/unlock?source=dashboard&member=Anna")
            .exchange()
            .expectStatus().isAccepted()
            .expectBody().isEmpty();
    }

    static Stream<Arguments> refusals() {
        return Stream.of(
            Arguments.of(new AmplifierException.Refused("The amplifier answered 200"), 502, "AMPLIFIER_REFUSED"),
            Arguments.of(new AmplifierException.Unavailable("The amplifier could not be reached"), 503, "AMPLIFIER_UNAVAILABLE"),
            Arguments.of(new AmplifierException.Timeout("The amplifier did not answer in time"), 504, "AMPLIFIER_TIMEOUT"),
            Arguments.of(new AmplifierException.InProgress(), 409, "UNLOCK_IN_PROGRESS"),
            Arguments.of(new AmplifierException.UnknownMember("Ola"), 400, "UNKNOWN_MEMBER"),
            Arguments.of(new AmplifierException.RegistryUnavailable(), 503, "REGISTRY_UNAVAILABLE"));
    }

    @ParameterizedTest
    @MethodSource("refusals")
    void should_answer_a_refusal_with_its_status_and_code(final AmplifierException refusal, final int status, final String code) {
        when(amplifierService.unlock(anyString(), anyString())).thenReturn(Mono.error(refusal));

        webTestClient.post().uri("/amplifier/unlock?source=dashboard&member=Anna")
            .exchange()
            .expectStatus().isEqualTo(status)
            .expectBody()
            .jsonPath("$.errors[0].code").isEqualTo(code);
    }

    @Test
    void should_refuse_another_source() {
        webTestClient.post().uri("/amplifier/unlock?source=panel&member=Anna")
            .exchange()
            .expectStatus().isBadRequest();

        verifyNoInteractions(amplifierService);
    }

    @Test
    void should_refuse_an_unlock_without_a_member() {
        webTestClient.post().uri("/amplifier/unlock?source=dashboard")
            .exchange()
            .expectStatus().isBadRequest();

        verifyNoInteractions(amplifierService);
    }

    //the unlock is a POST: a GET of the same path changes nothing
    @Test
    void should_not_unlock_on_a_get() {
        webTestClient.get().uri("/amplifier/unlock?source=dashboard&member=Anna")
            .exchange()
            .expectStatus().isEqualTo(405);

        verifyNoInteractions(amplifierService);
    }
}

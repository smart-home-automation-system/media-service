package cloud.cholewa.media.amplifier;

import cloud.cholewa.media.config.AmplifierProperties;
import cloud.cholewa.media.config.WebClientConfigAccess;
import cloud.cholewa.media.error.AmplifierError;
import cloud.cholewa.media.error.AmplifierException;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import mockwebserver3.SocketEffect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

//HAS-515: one POST of the firmware form, the redirect to the page of an invalid BCD file is the success. Against a
//MockWebServer - never the amplifier of the house
class AmplifierClientTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    private MockWebServer server;
    private AmplifierClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = clientOf("localhost:" + server.getPort());
    }

    @AfterEach
    void tearDown() {
        server.close();
    }

    private static AmplifierClient clientOf(final String address) {
        final WebClient webClient = WebClientConfigAccess.amplifierWebClient(
            WebClient.builder(), new AmplifierProperties(address, TIMEOUT));
        return new AmplifierClient(webClient);
    }

    @Test
    void should_unlock_on_the_redirect_to_the_page_of_an_invalid_file() throws InterruptedException {
        server.enqueue(redirect("/1000/firmware_update/bl_bcd_error.asp"));

        client.unlock().as(StepVerifier::create).verifyComplete();

        final RecordedRequest request = server.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getUrl().encodedPath()).isEqualTo("/goform/formPostHandler");
        assertThat(request.getHeaders().get(HttpHeaders.CONTENT_TYPE)).startsWith("multipart/form-data");
        assertThat(request.getBody().utf8())
            .contains("name=\"appFirmware\"; filename=\"unlock.txt\"")
            .contains("Content-Type: text/plain")
            .contains("name=\"appFirmwareFile\"")
            .contains("Upload");
    }

    //sent once: nothing is repeated, whatever the answer
    @Test
    void should_send_the_file_once() {
        server.enqueue(new MockResponse.Builder().code(500).build());

        client.unlock().as(StepVerifier::create).verifyError(AmplifierException.Refused.class);

        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void should_be_refused_on_a_200() {
        server.enqueue(new MockResponse.Builder().code(200).body("<html></html>").build());

        client.unlock().as(StepVerifier::create)
            .expectErrorSatisfies(e -> assertThat(((AmplifierException) e).getError()).isEqualTo(AmplifierError.AMPLIFIER_REFUSED))
            .verify();
    }

    @Test
    void should_be_refused_on_a_redirect_elsewhere() {
        server.enqueue(redirect("/bl_index.asp"));

        client.unlock().as(StepVerifier::create).verifyError(AmplifierException.Refused.class);
    }

    @Test
    void should_be_refused_on_a_redirect_without_a_location() {
        server.enqueue(new MockResponse.Builder().code(302).build());

        client.unlock().as(StepVerifier::create).verifyError(AmplifierException.Refused.class);
    }

    @Test
    void should_be_refused_on_a_closed_socket() {
        server.enqueue(new MockResponse.Builder().onRequestStart(new SocketEffect.CloseSocket()).build());

        client.unlock().as(StepVerifier::create).verifyError(AmplifierException.Refused.class);
    }

    //an answer that does not come may have been taken - AMPLIFIER_TIMEOUT, never AMPLIFIER_REFUSED
    @Test
    void should_time_out_without_an_answer() {
        server.enqueue(redirect("/bl_bcd_error.asp").newBuilder()
            .headersDelay(TIMEOUT.toMillis() * 3, TimeUnit.MILLISECONDS).build());

        client.unlock().as(StepVerifier::create)
            .expectErrorSatisfies(e -> assertThat(((AmplifierException) e).getError()).isEqualTo(AmplifierError.AMPLIFIER_TIMEOUT))
            .verify(Duration.ofSeconds(10));
    }

    @Test
    void should_be_unavailable_without_a_connection() throws IOException {
        final int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        clientOf("localhost:" + closedPort).unlock().as(StepVerifier::create)
            .expectErrorSatisfies(e -> assertThat(((AmplifierException) e).getError()).isEqualTo(AmplifierError.AMPLIFIER_UNAVAILABLE))
            .verify(Duration.ofSeconds(10));
    }

    private static MockResponse redirect(final String location) {
        return new MockResponse.Builder().code(302).addHeader(HttpHeaders.LOCATION, location).build();
    }
}

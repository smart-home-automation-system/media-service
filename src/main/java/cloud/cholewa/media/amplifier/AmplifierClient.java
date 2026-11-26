package cloud.cholewa.media.amplifier;

import cloud.cholewa.media.error.AmplifierException;
import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.List;

//The unlock of the amplifier (GoAhead-Webs, HTTP/1.0, port 80): one POST of the firmware form of its web page with a
//text file of a few bytes. The amplifier refuses the file as an invalid BCD file - a 302 to the page that says so - and
//works again (verified on the device on 2026-11-24). Sent once and never retried; the answer is read for its status
//and Location only, the body is drained. Its state cannot be read: the page is the same hung or healthy
@Slf4j
@Component
public class AmplifierClient {

    static final String FORM = "/goform/formPostHandler";
    static final String SUCCESS_PAGE = "/bl_bcd_error.asp";
    static final String FILE_NAME = "unlock.txt";
    static final byte[] FILE = "unlock\n".getBytes(StandardCharsets.US_ASCII);

    private static final List<Class<? extends Throwable>> NO_CONNECTION = List.of(
        ConnectException.class, ConnectTimeoutException.class, NoRouteToHostException.class, UnknownHostException.class);

    private final WebClient amplifierWebClient;

    public AmplifierClient(@Qualifier("amplifierWebClient") final WebClient amplifierWebClient) {
        this.amplifierWebClient = amplifierWebClient;
    }

    //completes empty once the amplifier took the file; AMPLIFIER_REFUSED for any other answer, AMPLIFIER_UNAVAILABLE
    //without a connection, AMPLIFIER_TIMEOUT without an answer in time
    public Mono<Void> unlock() {
        return amplifierWebClient
            .post()
            .uri(FORM)
            .contentType(MediaType.MULTIPART_FORM_DATA)
            .body(BodyInserters.fromMultipartData(form()))
            .exchangeToMono(response -> response.releaseBody().thenReturn(new Answer(
                response.statusCode().value(), response.headers().asHttpHeaders().getFirst(HttpHeaders.LOCATION))))
            .onErrorMap(AmplifierClient::mapped)
            .flatMap(answer -> Mono.just(answer)
                .filter(Answer::unlocked)
                .switchIfEmpty(Mono.error(() -> refused(answer))))
            .then();
    }

    private static MultiValueMap<String, HttpEntity<?>> form() {
        final MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("appFirmware", new ByteArrayResource(FILE) {
            @Override
            public String getFilename() {
                return FILE_NAME;
            }
        }).contentType(MediaType.TEXT_PLAIN);
        builder.part("appFirmwareFile", "Upload");
        return builder.build();
    }

    private static AmplifierException refused(final Answer answer) {
        log.warn("The amplifier answered {} with Location {} to the unlock", answer.status(), answer.location());
        return new AmplifierException.Refused("The amplifier answered " + answer.status());
    }

    //a connection never made sent nothing; an answer that did not come may have been taken; anything else after the
    //connection (a socket closed in the middle, a broken answer) is a refusal - sending the file again is harmless
    private static AmplifierException mapped(final Throwable failure) {
        if (causedBy(failure, NO_CONNECTION)) {
            log.warn("The amplifier could not be reached: {}", failure.toString());
            return new AmplifierException.Unavailable("The amplifier could not be reached");
        }
        if (causedBy(failure, List.of(ReadTimeoutException.class))) {
            log.warn("The amplifier did not answer the unlock in time");
            return new AmplifierException.Timeout("The amplifier did not answer in time");
        }
        log.warn("The unlock of the amplifier failed: {}", failure.toString());
        return new AmplifierException.Refused(failure instanceof WebClientRequestException
            ? "The amplifier closed the connection" : "The answer of the amplifier could not be read");
    }

    private static boolean causedBy(final Throwable failure, final List<Class<? extends Throwable>> types) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            final Throwable current = cause;
            if (types.stream().anyMatch(type -> type.isInstance(current))) {
                return true;
            }
        }
        return false;
    }

    //the status and the Location of the answer - the success is the redirect to the page of an invalid BCD file
    record Answer(int status, String location) {

        boolean unlocked() {
            return status == HttpStatus.FOUND.value() && location != null && location.endsWith(SUCCESS_PAGE);
        }
    }
}

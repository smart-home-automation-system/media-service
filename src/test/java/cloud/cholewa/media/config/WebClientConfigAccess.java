package cloud.cholewa.media.config;

import org.springframework.web.reactive.function.client.WebClient;

//the client of the amplifier exactly as the application builds it, for the tests of other packages
public final class WebClientConfigAccess {

    private WebClientConfigAccess() {
    }

    public static WebClient amplifierWebClient(final WebClient.Builder builder, final AmplifierProperties amplifier) {
        return new WebClientConfig().amplifierWebClient(builder, amplifier);
    }
}

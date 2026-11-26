package cloud.cholewa.media.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

//The clients of database-service (the household profiles) and of the amplifier, built from the injected builder so the
//calls are traced and logged by Logbook (cholewa-commons >= 1.10.0)
@Configuration
public class WebClientConfig {

    static final Duration DATABASE_RESPONSE_TIMEOUT = Duration.ofSeconds(5);
    static final int DATABASE_CONNECT_TIMEOUT_MILLIS = 5000;

    //Without a limit an idle connection stays in the pool for ever: after a restart of database-service, or once
    //conntrack has forgotten it, the next call is written onto a dead socket (the lesson of the gateway, HAS-212)
    static final Duration MAX_IDLE_TIME = Duration.ofMinutes(2);
    static final Duration MAX_LIFE_TIME = Duration.ofMinutes(30);

    //Spring infers only close() and shutdown() as destroy methods, so the pool is named explicitly
    @Bean(destroyMethod = "dispose")
    ConnectionProvider databaseServiceConnectionProvider() {
        return ConnectionProvider.builder("database-service")
            .maxIdleTime(MAX_IDLE_TIME)
            .maxLifeTime(MAX_LIFE_TIME)
            .build();
    }

    //the builder is a prototype bean: every client gets its own copy
    @Bean
    WebClient databaseServiceWebClient(
        final WebClient.Builder webClientBuilder,
        final ConnectionProvider databaseServiceConnectionProvider,
        final InternalServicesProperties services
    ) {
        final HttpClient httpClient = HttpClient.create(databaseServiceConnectionProvider)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, DATABASE_CONNECT_TIMEOUT_MILLIS)
            .responseTimeout(DATABASE_RESPONSE_TIMEOUT);
        return webClientBuilder.clone()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl(services.database().toString())
            .build();
    }

    //The amplifier speaks HTTP/1.0 (GoAhead-Webs) and closes the connection after every answer, so there is nothing to
    //pool: a new connection per unlock. Redirects are not followed - the redirect is the answer (reactor-netty follows
    //none by default; set here so a later default cannot change it). Both timeouts are media.amplifier.timeout: a
    //connection that is not made is AMPLIFIER_UNAVAILABLE, an answer that does not come AMPLIFIER_TIMEOUT
    @Bean
    WebClient amplifierWebClient(final WebClient.Builder webClientBuilder, final AmplifierProperties amplifier) {
        final HttpClient httpClient = HttpClient.newConnection()
            .followRedirect(false)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, (int) amplifier.timeout().toMillis())
            .responseTimeout(amplifier.timeout());
        return webClientBuilder.clone()
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .baseUrl("http://" + amplifier.address())
            .build();
    }
}

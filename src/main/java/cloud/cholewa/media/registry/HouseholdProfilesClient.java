package cloud.cholewa.media.registry;

import cloud.cholewa.commons.error.DownstreamErrors;
import cloud.cholewa.home.model.HouseholdProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

//The profiles of the household of database-service: the switched-on members by name, role and permissions, nothing
//else of the registry - no phone, no devices. Read to check the member who unlocks the amplifier (HAS-515). What a
//failing database-service says goes to the log, never further; no body at all is a failed call. The third copy of the
//client, after appliance-service and gate-service (CLAUDE.md)
@Slf4j
@Component
public class HouseholdProfilesClient {

    private static final ParameterizedTypeReference<List<HouseholdProfile>> PROFILES = new ParameterizedTypeReference<>() {
    };

    private final WebClient databaseServiceWebClient;

    public HouseholdProfilesClient(@Qualifier("databaseServiceWebClient") final WebClient databaseServiceWebClient) {
        this.databaseServiceWebClient = databaseServiceWebClient;
    }

    public Mono<List<HouseholdProfile>> getProfiles() {
        return databaseServiceWebClient
            .get()
            .uri("/home/household/profiles")
            .retrieve()
            .onStatus(HttpStatusCode::isError, response -> DownstreamErrors.read(response)
                .map(error -> {
                    log.warn("database-service answered {} to the read of the household profiles: {}",
                        error.status().value(), error.errors());
                    return new RegistryCallException("database-service answered " + error.status().value());
                }))
            .bodyToMono(PROFILES)
            .switchIfEmpty(Mono.error(() -> new RegistryCallException("database-service answered without a list")));
    }
}

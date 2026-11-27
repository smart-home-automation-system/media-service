package cloud.cholewa.media.amplifier;

import cloud.cholewa.home.model.HouseholdProfile;
import cloud.cholewa.media.error.AmplifierException;
import cloud.cholewa.media.registry.HouseholdProfilesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Locale;

//The member who presses the button of the amplifier, found among the household profiles of database-service: answers
//the name as the registry spells it - the case of the request does not matter, as the registry keeps names unique
//ignoring it. 400 UNKNOWN_MEMBER for a name no switched-on member has, 503 REGISTRY_UNAVAILABLE when the profiles cannot
//be read. A trace of who pressed, never a check of a role or a permission (owner, 2026-11-27, HAS-517): who is offered
//the button is the dashboard's to decide - the administrator, and a resident granted amplifier_unlock
@Component
@RequiredArgsConstructor
public class HouseholdMembers {

    private final HouseholdProfilesClient profilesClient;

    public Mono<String> known(final String member) {
        return profilesClient.getProfiles()
            .onErrorMap(failure -> !(failure instanceof AmplifierException), failure -> new AmplifierException.RegistryUnavailable())
            .flatMap(profiles -> Mono.justOrEmpty(profiles.stream()
                .filter(profile -> sameName(profile, member))
                .findFirst()))
            .switchIfEmpty(Mono.error(() -> new AmplifierException.UnknownMember(member)))
            .map(HouseholdProfile::getName);
    }

    //the name is required by the schema of the profile (@Nonnull of the SDK)
    private static boolean sameName(final HouseholdProfile profile, final String member) {
        return profile.getName().toUpperCase(Locale.ROOT).equals(member.toUpperCase(Locale.ROOT));
    }
}

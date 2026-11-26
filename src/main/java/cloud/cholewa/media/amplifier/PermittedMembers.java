package cloud.cholewa.media.amplifier;

import cloud.cholewa.home.model.HouseholdProfile;
import cloud.cholewa.home.model.MemberPermission;
import cloud.cholewa.media.error.AmplifierException;
import cloud.cholewa.media.registry.HouseholdProfilesClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Locale;

//The member who presses the button of the amplifier, checked against the household profiles of database-service: answers
//the name as the registry spells it - the case of the request does not matter, as the registry keeps names unique
//ignoring it. 400 UNKNOWN_MEMBER for a name no switched-on member has, 403 NOT_PERMITTED without amplifier_unlock, 503
//REGISTRY_UNAVAILABLE when the profiles cannot be read. A trace of who pressed and a guard against a mistake, not access
//control - nothing behind the gateway is authenticated
@Component
@RequiredArgsConstructor
public class PermittedMembers {

    private final HouseholdProfilesClient profilesClient;

    public Mono<String> permitted(final String member) {
        return profilesClient.getProfiles()
            .onErrorMap(failure -> !(failure instanceof AmplifierException), failure -> new AmplifierException.RegistryUnavailable())
            .flatMap(profiles -> Mono.justOrEmpty(profiles.stream()
                .filter(profile -> sameName(profile, member))
                .findFirst()))
            .switchIfEmpty(Mono.error(() -> new AmplifierException.UnknownMember(member)))
            .filter(PermittedMembers::mayUnlock)
            .switchIfEmpty(Mono.error(() -> new AmplifierException.NotPermitted(member)))
            .map(HouseholdProfile::getName);
    }

    private static boolean sameName(final HouseholdProfile profile, final String member) {
        return profile.getName() != null
            && profile.getName().toUpperCase(Locale.ROOT).equals(member.toUpperCase(Locale.ROOT));
    }

    private static boolean mayUnlock(final HouseholdProfile profile) {
        return profile.getPermissions() != null && profile.getPermissions().contains(MemberPermission.AMPLIFIER_UNLOCK);
    }
}

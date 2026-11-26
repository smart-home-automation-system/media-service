package cloud.cholewa.media.amplifier;

import cloud.cholewa.home.model.HouseholdProfile;
import cloud.cholewa.home.model.MemberPermission;
import cloud.cholewa.home.model.MemberRole;
import cloud.cholewa.media.error.AmplifierException;
import cloud.cholewa.media.registry.HouseholdProfilesClient;
import cloud.cholewa.media.registry.RegistryCallException;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

//HAS-515: only a switched-on member with amplifier_unlock may unlock the amplifier
class PermittedMembersTest {

    private final HouseholdProfilesClient profilesClient = mock(HouseholdProfilesClient.class);
    private final PermittedMembers sut = new PermittedMembers(profilesClient);

    @Test
    void should_answer_the_name_as_the_registry_spells_it() {
        when(profilesClient.getProfiles()).thenReturn(Mono.just(List.of(
            profile("Jan", MemberPermission.HEATING_SWITCH),
            profile("Anna", MemberPermission.HEATING_SWITCH, MemberPermission.AMPLIFIER_UNLOCK))));

        sut.permitted("anna").as(StepVerifier::create).expectNext("Anna").verifyComplete();
    }

    @Test
    void should_refuse_an_unknown_member() {
        when(profilesClient.getProfiles()).thenReturn(Mono.just(List.of(profile("Anna", MemberPermission.AMPLIFIER_UNLOCK))));

        sut.permitted("Ola").as(StepVerifier::create).verifyError(AmplifierException.UnknownMember.class);
    }

    @Test
    void should_refuse_a_member_without_the_permission() {
        when(profilesClient.getProfiles()).thenReturn(Mono.just(List.of(profile("Jan", MemberPermission.HEATING_SWITCH))));

        sut.permitted("Jan").as(StepVerifier::create).verifyError(AmplifierException.NotPermitted.class);
    }

    //a profile the registry answers without the list holds no permission - missing is never granted
    @Test
    void should_refuse_a_member_without_any_permission() {
        when(profilesClient.getProfiles()).thenReturn(Mono.just(List.of(
            new HouseholdProfile().name("Jan").role(MemberRole.RESIDENT).permissions(null))));

        sut.permitted("Jan").as(StepVerifier::create).verifyError(AmplifierException.NotPermitted.class);
    }

    @Test
    void should_refuse_when_the_profiles_cannot_be_read() {
        when(profilesClient.getProfiles()).thenReturn(Mono.error(new RegistryCallException("database-service answered 503")));

        sut.permitted("Anna").as(StepVerifier::create).verifyError(AmplifierException.RegistryUnavailable.class);
    }

    private static HouseholdProfile profile(final String name, final MemberPermission... permissions) {
        return new HouseholdProfile().name(name).role(MemberRole.RESIDENT).permissions(List.of(permissions));
    }
}

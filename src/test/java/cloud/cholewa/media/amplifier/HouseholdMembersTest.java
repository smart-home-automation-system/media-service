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

//HAS-515: the member who presses is a switched-on member of the household - HAS-517 (owner, 2026-11-27): whatever their
//role or permissions, since the backend blocks nothing by them
class HouseholdMembersTest {

    private final HouseholdProfilesClient profilesClient = mock(HouseholdProfilesClient.class);
    private final HouseholdMembers sut = new HouseholdMembers(profilesClient);

    @Test
    void should_answer_the_name_as_the_registry_spells_it() {
        when(profilesClient.getProfiles()).thenReturn(Mono.just(List.of(
            profile("Jan", MemberRole.RESIDENT, MemberPermission.HEATING_SWITCH),
            profile("Anna", MemberRole.RESIDENT, MemberPermission.AMPLIFIER_UNLOCK))));

        sut.known("anna").as(StepVerifier::create).expectNext("Anna").verifyComplete();
    }

    @Test
    void should_refuse_an_unknown_member() {
        when(profilesClient.getProfiles()).thenReturn(Mono.just(List.of(
            profile("Anna", MemberRole.RESIDENT, MemberPermission.AMPLIFIER_UNLOCK))));

        sut.known("Ola").as(StepVerifier::create).verifyError(AmplifierException.UnknownMember.class);
    }

    //the administrator is offered the button on the media page without the permission
    @Test
    void should_let_the_administrator_unlock_without_the_permission() {
        when(profilesClient.getProfiles()).thenReturn(Mono.just(List.of(profile("Kris", MemberRole.ADMIN))));

        sut.known("Kris").as(StepVerifier::create).expectNext("Kris").verifyComplete();
    }

    //nor is a resident refused for a missing permission: what a member is offered is the dashboard's
    @Test
    void should_not_judge_the_permissions_of_a_member() {
        when(profilesClient.getProfiles()).thenReturn(Mono.just(List.of(
            profile("Jan", MemberRole.RESIDENT, MemberPermission.HEATING_SWITCH),
            new HouseholdProfile().name("Ola").role(MemberRole.RESIDENT).permissions(null))));

        sut.known("Jan").as(StepVerifier::create).expectNext("Jan").verifyComplete();
        sut.known("Ola").as(StepVerifier::create).expectNext("Ola").verifyComplete();
    }

    @Test
    void should_refuse_when_the_profiles_cannot_be_read() {
        when(profilesClient.getProfiles()).thenReturn(Mono.error(new RegistryCallException("database-service answered 503")));

        sut.known("Anna").as(StepVerifier::create).verifyError(AmplifierException.RegistryUnavailable.class);
    }

    private static HouseholdProfile profile(final String name, final MemberRole role, final MemberPermission... permissions) {
        return new HouseholdProfile().name(name).role(role).permissions(List.of(permissions));
    }
}

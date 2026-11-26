package cloud.cholewa.media.amplifier;

import cloud.cholewa.media.error.AmplifierException;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

//HAS-515: the member first, then one call to the amplifier at a time
class AmplifierServiceTest {

    private final PermittedMembers permittedMembers = mock(PermittedMembers.class);
    private final AmplifierClient amplifierClient = mock(AmplifierClient.class);
    private final AmplifierService sut = new AmplifierService(permittedMembers, amplifierClient);

    @Test
    void should_unlock_for_a_permitted_member() {
        when(permittedMembers.permitted("anna")).thenReturn(Mono.just("Anna"));
        when(amplifierClient.unlock()).thenReturn(Mono.empty());

        sut.unlock("dashboard", "anna").as(StepVerifier::create).verifyComplete();

        verify(amplifierClient).unlock();
    }

    @Test
    void should_send_nothing_for_a_member_who_is_refused() {
        when(permittedMembers.permitted(anyString())).thenReturn(Mono.error(new AmplifierException.NotPermitted("Jan")));

        sut.unlock("dashboard", "Jan").as(StepVerifier::create).verifyError(AmplifierException.NotPermitted.class);

        verify(amplifierClient, never()).unlock();
    }

    //a second unlock while the first is on its way is refused, never queued nor sent twice
    @Test
    void should_refuse_a_second_unlock_while_the_first_is_on_its_way() {
        final Sinks.Empty<Void> firstAnswer = Sinks.empty();
        when(permittedMembers.permitted(anyString())).thenReturn(Mono.just("Anna"));
        when(amplifierClient.unlock()).thenReturn(firstAnswer.asMono());

        sut.unlock("dashboard", "Anna").as(StepVerifier::create)
            .then(() -> sut.unlock("dashboard", "Anna").as(StepVerifier::create)
                .verifyError(AmplifierException.InProgress.class))
            .then(firstAnswer::tryEmitEmpty)
            .verifyComplete();

        verify(amplifierClient, times(1)).unlock();
    }

    //the guard is given back after a failure, so the next press is sent
    @Test
    void should_send_the_next_unlock_after_a_failed_one() {
        when(permittedMembers.permitted(anyString())).thenReturn(Mono.just("Anna"));
        when(amplifierClient.unlock())
            .thenReturn(Mono.error(new AmplifierException.Timeout("The amplifier did not answer in time")))
            .thenReturn(Mono.empty());

        sut.unlock("dashboard", "Anna").as(StepVerifier::create).verifyError(AmplifierException.Timeout.class);
        sut.unlock("dashboard", "Anna").as(StepVerifier::create).verifyComplete();

        verify(amplifierClient, times(2)).unlock();
    }

    //a caller that gives up while the amplifier is called gives the guard back too
    @Test
    void should_send_the_next_unlock_after_a_cancelled_one() {
        when(permittedMembers.permitted(anyString())).thenReturn(Mono.just("Anna"));
        when(amplifierClient.unlock()).thenReturn(Mono.never());

        final Disposable first = sut.unlock("dashboard", "Anna").subscribe();
        verify(amplifierClient).unlock();
        first.dispose();

        when(amplifierClient.unlock()).thenReturn(Mono.empty());
        sut.unlock("dashboard", "Anna").as(StepVerifier::create).verifyComplete();
    }

    //a caller that gives up before the amplifier is called - the guard taken, the call not made yet - gives it back too
    @Test
    void should_send_the_next_unlock_after_one_cancelled_before_the_call() {
        final Sinks.One<String> member = Sinks.one();
        when(permittedMembers.permitted(anyString())).thenReturn(member.asMono()).thenReturn(Mono.just("Anna"));
        when(amplifierClient.unlock()).thenReturn(Mono.empty());

        final Disposable first = sut.unlock("dashboard", "Anna").subscribe();
        first.dispose();
        member.tryEmitValue("Anna");

        sut.unlock("dashboard", "Anna").as(StepVerifier::create).verifyComplete();
    }
}

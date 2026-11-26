package cloud.cholewa.media.amplifier;

import cloud.cholewa.media.error.AmplifierException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

//The unlock of the amplifier pressed by a member (HAS-515): the member checked first, then one call to the amplifier at
//a time - a second unlock while the first is on its way is 409 UNLOCK_IN_PROGRESS, never queued nor sent twice. The
//guard is a resource of Mono.using: taken when the call is subscribed and given back once it completes, fails or is
//cancelled - also a cancel that comes before the amplifier is called, which would otherwise keep the guard for good.
//Every unlock is an INFO line with the member, the source and the outcome
@Slf4j
@Service
@RequiredArgsConstructor
public class AmplifierService {

    private final PermittedMembers permittedMembers;
    private final AmplifierClient amplifierClient;

    private final AtomicBoolean inProgress = new AtomicBoolean();

    public Mono<Void> unlock(final String source, final String member) {
        return permittedMembers.permitted(member)
            .flatMap(name -> Mono.using(
                    () -> inProgress.compareAndSet(false, true),
                    taken -> Mono.just(taken)
                        .filter(Boolean::booleanValue)
                        .switchIfEmpty(Mono.error(AmplifierException.InProgress::new))
                        .then(Mono.defer(amplifierClient::unlock)),
                    this::giveBack)
                .doOnSuccess(ignored -> log.info("The amplifier was unlocked by {} from the {}", name, source)))
            .doOnError(failure -> log.info("The unlock of the amplifier by {} from the {} was refused: {}",
                member, source, failure instanceof AmplifierException refused ? refused.getError().name() : failure.toString()));
    }

    //only the unlock that took the guard gives it back
    private void giveBack(final boolean taken) {
        if (taken) {
            inProgress.set(false);
        }
    }
}

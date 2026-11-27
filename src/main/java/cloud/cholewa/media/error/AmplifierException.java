package cloud.cholewa.media.error;

import lombok.Getter;

//An unlock the API refuses, by its cause; one subclass per cause, so each has its own DomainExceptionProcessor of
//cholewa-commons with its status (ExceptionHandlerConfig). The message is answered as details for a 4xx only - never
//what the amplifier or database-service said
@Getter
public abstract class AmplifierException extends RuntimeException {

    private final AmplifierError error;

    protected AmplifierException(final AmplifierError error, final String message) {
        super(message);
        this.error = error;
    }

    public static final class Refused extends AmplifierException {
        public Refused(final String reason) {
            super(AmplifierError.AMPLIFIER_REFUSED, reason);
        }
    }

    public static final class Unavailable extends AmplifierException {
        public Unavailable(final String reason) {
            super(AmplifierError.AMPLIFIER_UNAVAILABLE, reason);
        }
    }

    public static final class Timeout extends AmplifierException {
        public Timeout(final String reason) {
            super(AmplifierError.AMPLIFIER_TIMEOUT, reason);
        }
    }

    public static final class InProgress extends AmplifierException {
        public InProgress() {
            super(AmplifierError.UNLOCK_IN_PROGRESS, "Another unlock of the amplifier is on its way");
        }
    }

    public static final class UnknownMember extends AmplifierException {
        public UnknownMember(final String member) {
            super(AmplifierError.UNKNOWN_MEMBER, "No switched-on household member is named " + member);
        }
    }

    public static final class RegistryUnavailable extends AmplifierException {
        public RegistryUnavailable() {
            super(AmplifierError.REGISTRY_UNAVAILABLE, "The household profiles could not be read");
        }
    }
}

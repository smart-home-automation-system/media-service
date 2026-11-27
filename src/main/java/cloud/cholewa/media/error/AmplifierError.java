package cloud.cholewa.media.error;

import cloud.cholewa.commons.error.model.ErrorId;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

//The causes of an unlock the API refuses. Their names are the codes of the answer (ErrorId.codeOf) - wire contract for
//the dashboard, which words each of them (HAS-517); pinned by AmplifierErrorTest
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum AmplifierError implements ErrorId {

    //the amplifier answered, but not with the redirect to the page of an invalid file - the unlock did not happen
    AMPLIFIER_REFUSED("Amplifier refused"),

    //no connection to the amplifier - nothing was sent
    AMPLIFIER_UNAVAILABLE("Amplifier unavailable"),

    //no answer within media.amplifier.timeout - the file may have been taken all the same
    AMPLIFIER_TIMEOUT("Amplifier timeout"),

    //another unlock is on its way to the amplifier
    UNLOCK_IN_PROGRESS("Unlock in progress"),

    //the member who pressed the button is no switched-on member of the household registry
    UNKNOWN_MEMBER("Unknown household member"),

    //the household profiles could not be read, so the member cannot be checked
    REGISTRY_UNAVAILABLE("Registry unavailable");

    private final String description;
}

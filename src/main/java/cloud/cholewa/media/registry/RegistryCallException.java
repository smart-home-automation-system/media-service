package cloud.cholewa.media.registry;

//a read of the household profiles of database-service that did not end with the list asked for; never answered as
//such - it is REGISTRY_UNAVAILABLE of an unlock
public class RegistryCallException extends RuntimeException {

    public RegistryCallException(final String message) {
        super(message);
    }
}

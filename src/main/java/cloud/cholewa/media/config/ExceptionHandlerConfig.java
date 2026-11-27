package cloud.cholewa.media.config;

import cloud.cholewa.commons.error.GlobalErrorExceptionHandler;
import cloud.cholewa.commons.error.processor.DomainExceptionProcessor;
import cloud.cholewa.media.error.AmplifierError;
import cloud.cholewa.media.error.AmplifierException;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

//The error contract of the organization (cholewa-commons) with one processor per cause of a refused unlock (HAS-515)
@Configuration
public class ExceptionHandlerConfig {

    @Bean
    @Order(-2)
    public GlobalErrorExceptionHandler globalErrorExceptionHandler(
        final ErrorAttributes errorAttributes,
        final WebProperties webProperties,
        final ApplicationContext applicationContext,
        final ServerCodecConfigurer serverCodecConfigurer
    ) {
        return new GlobalErrorExceptionHandler(
            errorAttributes, webProperties.getResources(), applicationContext, serverCodecConfigurer
        ).withCustomErrorProcessor(Map.of(
            AmplifierException.Refused.class, new DomainExceptionProcessor(BAD_GATEWAY, AmplifierError.AMPLIFIER_REFUSED),
            AmplifierException.Unavailable.class,
            new DomainExceptionProcessor(SERVICE_UNAVAILABLE, AmplifierError.AMPLIFIER_UNAVAILABLE),
            AmplifierException.Timeout.class, new DomainExceptionProcessor(GATEWAY_TIMEOUT, AmplifierError.AMPLIFIER_TIMEOUT),
            AmplifierException.InProgress.class, new DomainExceptionProcessor(CONFLICT, AmplifierError.UNLOCK_IN_PROGRESS),
            AmplifierException.UnknownMember.class, new DomainExceptionProcessor(BAD_REQUEST, AmplifierError.UNKNOWN_MEMBER),
            AmplifierException.RegistryUnavailable.class,
            new DomainExceptionProcessor(SERVICE_UNAVAILABLE, AmplifierError.REGISTRY_UNAVAILABLE)
        ));
    }
}

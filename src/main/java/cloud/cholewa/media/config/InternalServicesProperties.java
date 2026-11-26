package cloud.cholewa.media.config;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.net.URI;

/**
 * The services this one calls inside the cluster, by k8s DNS. {@code database} - database-service: the household
 * profiles the member who presses the button of the amplifier is checked against (HAS-515). No default: a made-up
 * address would start the service and fail every unlock.
 */
@Validated
@ConfigurationProperties("internal.service")
public record InternalServicesProperties(
    @NotNull URI database
) {
}

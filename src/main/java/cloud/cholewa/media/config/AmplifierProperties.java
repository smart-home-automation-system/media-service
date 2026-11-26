package cloud.cholewa.media.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * The amplifier of the house (HAS-515). {@code address} - its host on the LAN, from the manifest of deployment-tools; no
 * default, refused at the start without it by cholewa.required-properties, and never in this public repository.
 * {@code timeout} - how long the unlock waits for the answer of the amplifier, connecting included (a bare number is
 * seconds, 1 - 30 s); one checked on 2026-11-24 answered in 1.2 s.
 */
@Validated
@ConfigurationProperties("media.amplifier")
public record AmplifierProperties(
    @NotBlank String address,
    @NotNull @DurationUnit(ChronoUnit.SECONDS) @DurationMin(seconds = 1) @DurationMax(seconds = 30) Duration timeout
) {
}

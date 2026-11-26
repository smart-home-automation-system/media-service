package cloud.cholewa.media;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class MediaServiceApplicationTest {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    //the cluster ports of every service and the probe groups the manifest asks for - a 404 on the readiness path
    //crash-loops the pod
    @Test
    void should_listen_on_the_cluster_ports_with_the_probes() {
        assertThat(environment.getProperty("server.port")).isEqualTo("6200");
        assertThat(environment.getProperty("management.server.port")).isEqualTo("8200");
        assertThat(environment.getProperty("management.endpoint.health.probes.enabled", Boolean.class)).isTrue();
        assertThat(environment.getProperty("management.endpoints.web.exposure.include"))
            .isEqualTo("health,info,prometheus");
        assertThat(environment.getProperty("spring.webflux.base-path")).isEqualTo("/home/media");
    }

    //the address of the amplifier has no default: the start is refused without the variable (HAS-515), and a context test
    //points it at an address nothing listens on
    @Test
    void should_require_the_address_of_the_amplifier() {
        assertThat(environment.getProperty("cholewa.required-properties[0].property")).isEqualTo("media.amplifier.address");
        assertThat(environment.getProperty("cholewa.required-properties[0].variable")).isEqualTo("amplifier-address");
        assertThat(environment.getProperty("media.amplifier.address")).isEqualTo("localhost:1");
        assertThat(environment.getProperty("media.amplifier.timeout")).isEqualTo("PT5S");
        assertThat(environment.getProperty("internal.service.database")).isEqualTo("http://localhost:1");
    }

    //every trace is kept: the trace in the logs is the only record of a request
    @Test
    void should_sample_every_trace() {
        assertThat(environment.getProperty("management.tracing.sampling.probability", Double.class)).isEqualTo(1.0);
        assertThat(environment.getProperty("spring.reactor.context-propagation")).isEqualTo("auto");
    }
}

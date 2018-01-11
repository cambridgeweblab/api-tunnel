package ucles.weblab.common.tunnel.client;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import javax.annotation.PostConstruct;

/**
 * Settings for tunnel configuration properties.
 *
 * @since 01/08/2016
 */
@ConfigurationProperties(prefix = "weblab.tunnel.client")
@Data
public class TunnelSettings {
    private boolean enabled;
    private URI baseUrl;
    private URI webSocketPath;
    private long idleTimeout = 0L;

    @PostConstruct
    void validate() {
        if (enabled && (baseUrl == null || webSocketPath == null)) {
            throw new IllegalStateException("Tunnel enabled but baseUrl and path not both specified");
        }
    }
}

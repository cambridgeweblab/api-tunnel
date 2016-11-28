package ucles.weblab.common.tunnel.server;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;

/**
 * Settings for tunnel configuration properties.
 *
 * @since 01/08/2016
 */
@ConfigurationProperties(prefix = "weblab.tunnel.server")
@Data
public class TunnelSettings {
    private boolean enabled;
}

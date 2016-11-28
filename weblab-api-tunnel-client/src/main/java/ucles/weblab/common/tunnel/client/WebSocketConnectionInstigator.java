package ucles.weblab.common.tunnel.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

/**
 * Instigate a web socket connection.
 *
 * @since 26/08/2016
 */
@Slf4j
public class WebSocketConnectionInstigator {
    private static final long TICK_MILLIS = 60000;
    static final int ATTEMPTS_PER_TICK = 3;

    static WebSocketConnectionInstigator instance;
    private final URI webSocketUrl;
    private final WebSocketContainer webSocketContainer;
    private Session currentSession;

    public WebSocketConnectionInstigator(TunnelSettings settings, WebSocketContainer webSocketContainer) throws URISyntaxException {
        this.webSocketContainer = webSocketContainer;
        URI baseUrl = settings.getBaseUrl();
        // If the baseUrl is configured as http[s], then use ws[s] instead.
        if (baseUrl.getScheme().startsWith("http")) {
            final String wsScheme = baseUrl.getScheme().replace("http", "ws");
            baseUrl = new URI(wsScheme, baseUrl.getUserInfo(), baseUrl.getHost(), baseUrl.getPort(),
                    baseUrl.getPath(), baseUrl.getQuery(), baseUrl.getFragment());
        }
        final URI webSocketPath = settings.getWebSocketPath();
        this.webSocketUrl = baseUrl.resolve(webSocketPath);
        webSocketContainer.setDefaultMaxSessionIdleTimeout(0L);
    }

    @EventListener(ContextRefreshedEvent.class)
    protected void connectAtStartup() {
        instance = this;
        establishConnection();
    }

    @Scheduled(fixedDelay = TICK_MILLIS, initialDelay = TICK_MILLIS)
    protected void checkSession() {
        synchronized (this) {
            if (currentSession == null || !currentSession.isOpen()) {
                establishConnection();
            }
        }
    }

    public void reconnect() throws IOException {
        synchronized (this) {
            if (currentSession != null && currentSession.isOpen()) {
                currentSession = null;
            }
            establishConnection();
        }
    }

    private void establishConnection() {
        log.info("Establishing web socket connection to: " + webSocketUrl);
        int i = 0;
        do {
            try {
                currentSession = webSocketContainer.connectToServer(HttpTunnelSocketClient.class, webSocketUrl);
            } catch (DeploymentException | IOException e) {
                currentSession = null;
                if (i >= ATTEMPTS_PER_TICK - 1) {
                    log.warn("Failed to establish web socket connection. Retrying later...", e);
                }
            }
        } while ((currentSession == null || !currentSession.isOpen()) && ++i < ATTEMPTS_PER_TICK);
    }
}

package ucles.weblab.common.tunnel.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is ...
 *
 * @since 25/11/2016
 */
@RequiredArgsConstructor
@Slf4j
public class TunnelRestTemplateFactory {
    private final List<HttpMessageConverter<?>> messageConverterList;
    private final WebSocketResponseHandler webSocketResponseHandler;
    private final Map<String, WebSocketSession> webSocketSessions = new ConcurrentHashMap<>();

    private void configureMessageConverters(final RestTemplate restTemplate) {
        restTemplate.setMessageConverters(messageConverterList);
    }

    @EventListener(HttpTunnelUpEvent.class)
    public void handleTunnelUp(HttpTunnelUpEvent e) {
        webSocketSessions.put(e.getName(), e.getWebSocketSession());
    }

    @EventListener(HttpTunnelDownEvent.class)
    public void handleTunnelDown(HttpTunnelDownEvent e) {
        if (!webSocketSessions.remove(e.getName(), e.getWebSocketSession())) {
            log.debug("Ignoring tunnel down event as current session for {} does not match", e.getName());
        }
    }

    public RestTemplate getRestTemplateForSource(String externalSourceName) {
        final RestTemplate restTemplate = createRestTemplate(externalSourceName);

        if (restTemplate == null) {
            throw new ResourceAccessException("Web Socket tunnel not connected for " + externalSourceName);
        }
        return restTemplate;
    }

    private RestTemplate createRestTemplate(String name) {
        final WebSocketSession webSocketSession = webSocketSessions.get(name);
        if (webSocketSession == null) return null;
        else {
            log.debug("Create new RestTemplate for tunnel {}", name);
            final WebSocketHttpRequestFactory requestFactory = new WebSocketHttpRequestFactory(webSocketSession, webSocketResponseHandler);
            final RestTemplate restTemplate = new RestTemplate(requestFactory);
            configureMessageConverters(restTemplate);
            return restTemplate;
        }
    }

}

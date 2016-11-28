package ucles.weblab.common.tunnel.server;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is ...
 *
 * @since 25/11/2016
 */
@RequiredArgsConstructor
public class TunnelRestTemplateFactory {
    private final List<HttpMessageConverter<?>> messageConverterList;
    private final WebSocketResponseHandler webSocketResponseHandler;
    private final Map<String, RestTemplate> restTemplates = new ConcurrentHashMap<>();

    private void configureMessageConverters(final RestTemplate restTemplate) {
        restTemplate.setMessageConverters(messageConverterList);
    }

    @EventListener(HttpTunnelUpEvent.class)
    public void handleTunnelUp(HttpTunnelUpEvent e) {
        final WebSocketHttpRequestFactory requestFactory = new WebSocketHttpRequestFactory(e.getWebSocketSession(), webSocketResponseHandler);
        final RestTemplate restTemplate = new RestTemplate(requestFactory);
        configureMessageConverters(restTemplate);
        restTemplates.put(e.getName(), restTemplate);
    }

    @EventListener(HttpTunnelDownEvent.class)
    public void handleTunnelDown(HttpTunnelDownEvent e) {
        restTemplates.remove(e.getName());
    }

    public RestTemplate getRestTemplateForSource(String externalSourceName) {
        final RestTemplate restTemplate;
        restTemplate = restTemplates.get(externalSourceName);
        if (restTemplate == null) {
            throw new ResourceAccessException("Web Socket tunnel not connected for " + externalSourceName);
        }
        return restTemplate;
    }

}

package ucles.weblab.common.tunnel.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.PathMatcher;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * @since 25/11/2016
 */
@Configuration("server.webSocketTunnelConfiguration")
@ConditionalOnProperty(name = "weblab.tunnel.server.enabled", havingValue = "true")
public class WebSocketTunnelConfiguration {

    @Bean
    public HttpTunnelSocket httpTunnelSocket(PathMatcher pathMatcher, ApplicationEventPublisher messageBus) {
        return new HttpTunnelSocket(pathMatcher, messageBus);
    }

    @Bean
    public TunnelRestTemplateFactory tunnelRestTemplateFactory(RestTemplate restTemplate, WebSocketResponseHandler webSocketResponseHandler) {
        return new TunnelRestTemplateFactory(restTemplate.getMessageConverters(), webSocketResponseHandler);
    }

    @Bean
    public WebSocketResponseHandler webSocketResponseHandler() {
        return new WebSocketResponseHandler();
    }

    @Configuration
    @EnableWebSocket
    static class WebSocketConfig implements WebSocketConfigurer {
        @Autowired
        HttpTunnelSocket httpTunnelSocket;

        @Override
        public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
            // By default there is a same-origin policy on the socket - we can spoof for server-to-server, but we need to set allowed origins for browser testing.
            registry.addHandler(httpTunnelSocket, HttpTunnelSocket.TUNNEL_PATH).setAllowedOrigins("*");
        }
    }
}

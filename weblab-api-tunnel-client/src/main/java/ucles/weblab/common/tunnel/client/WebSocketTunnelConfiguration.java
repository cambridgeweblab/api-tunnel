package ucles.weblab.common.tunnel.client;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.URISyntaxException;
import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

@Configuration("client.webSocketTunnelConfiguration")
@ConditionalOnProperty(name = "weblab.tunnel.client.enabled", havingValue = "true")
@EnableScheduling
public class WebSocketTunnelConfiguration {

    @Bean
    TunnelSettings tunnelSettings() {
        return new TunnelSettings();
    }

    @Bean
    public WebSocketContainer webSocketContainer() {
        return ContainerProvider.getWebSocketContainer();
    }

    @Bean
    public WebSocketConnectionInstigator webSocketConnectionInstigator(TunnelSettings settings, WebSocketContainer webSocketContainer) throws URISyntaxException {
        return new WebSocketConnectionInstigator(settings, webSocketContainer);
    }

    @Bean
    @ConditionalOnWebApplication
    public ControllerIntrospectingTunnelledQueryHandler tunnelledQueryHandler(
            RequestMappingHandlerMapping requestMappingHandlerMapping, PathMatcher pathMatcher,
            ConversionService conversionService, MappingJackson2HttpMessageConverter jacksonConverter) {
        return new ControllerIntrospectingTunnelledQueryHandler(jacksonConverter.getObjectMapper(), requestMappingHandlerMapping, pathMatcher,
                conversionService);
    }
}

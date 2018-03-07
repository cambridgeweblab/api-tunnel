package ucles.weblab.common.tunnel.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;
import java.net.URI;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

/**
 * @since 07/09/2016
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        classes = { WebSocketConnectionInstigator_IT.Config.class, WebSocketTunnelConfiguration.class },
        webEnvironment = NONE)
@TestPropertySource(locations = "classpath:test.properties")
public class WebSocketConnectionInstigator_IT {
    @Configuration
    @EnableConfigurationProperties
    static class Config {
        @Bean
        public Session session() {
            Session session = mock(Session.class);
            when(session.isOpen()).thenReturn(true);
            return session;
        }

        @Bean
        @Primary
        public WebSocketContainer mockWebSocketContainer(Session session) throws IOException, DeploymentException {
            final WebSocketContainer webSocketContainer = mock(WebSocketContainer.class);
            when(webSocketContainer.connectToServer(any(), any(URI.class))).thenReturn(session);
            return webSocketContainer;
        }
    }

    @Autowired
    WebSocketContainer mockWebSocketContainer;

    @Test
    public void testContextRefreshEstablishesConnection() throws IOException, DeploymentException {
        // Called once during context initialisation
        verify(mockWebSocketContainer).connectToServer(HttpTunnelSocketClient.class, URI.create("ws://foo.bar/wibble"));
    }

}

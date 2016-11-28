package ucles.weblab.common.tunnel.server;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * @since 22/08/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpTunnelSocketTest {
    @Mock
    WebSocketSession webSocketSession;
    @Mock
    ApplicationEventPublisher messageBus;
    @Captor
    ArgumentCaptor<? extends ApplicationEvent> messagePublish;

    private final PathMatcher pathMatcher = new AntPathMatcher();

    private HttpTunnelSocket httpTunnelSocket;

    @Before
    public void setUp() {
        httpTunnelSocket = new HttpTunnelSocket(pathMatcher, messageBus);
    }

    @Test
    public void testUpEvent() throws Exception {
        when(webSocketSession.getUri()).thenReturn(URI.create("/prefix/externalsystems/simon/httpTunnel"));
        doNothing().when(messageBus).publishEvent(messagePublish.capture());

        long earliestEventTime = System.currentTimeMillis();
        httpTunnelSocket.afterConnectionEstablished(webSocketSession);
        long latestEventTime = System.currentTimeMillis();

        final ApplicationEvent event = messagePublish.getValue();
        assertThat("Expect up event", event, instanceOf(HttpTunnelUpEvent.class));
        final HttpTunnelUpEvent upEvent = (HttpTunnelUpEvent) event;
        assertSame("Expect session on event", webSocketSession, upEvent.getWebSocketSession());
        assertEquals("Expect name on event", "simon", upEvent.getName());
        assertSame("Expect source on event", httpTunnelSocket, upEvent.getSource());
        assertThat("Expect timestamp on event", upEvent.getTimestamp(),
                allOf(greaterThanOrEqualTo(earliestEventTime), lessThanOrEqualTo(latestEventTime)));
    }

    @Test
    public void testDownEvent() throws Exception {
        when(webSocketSession.getUri()).thenReturn(URI.create("/prefix/externalsystems/aisling/httpTunnel"));
        doNothing().when(messageBus).publishEvent(messagePublish.capture());

        long earliestEventTime = System.currentTimeMillis();
        httpTunnelSocket.afterConnectionClosed(webSocketSession, CloseStatus.NORMAL);
        long latestEventTime = System.currentTimeMillis();

        final ApplicationEvent event = messagePublish.getValue();
        assertThat("Expect up event", event, instanceOf(HttpTunnelDownEvent.class));
        final HttpTunnelDownEvent downEvent = (HttpTunnelDownEvent) event;
        assertEquals("Expect name on event", "aisling", downEvent.getName());
        assertSame("Expect source on event", httpTunnelSocket, downEvent.getSource());
        assertThat("Expect timestamp on event", downEvent.getTimestamp(),
                allOf(greaterThanOrEqualTo(earliestEventTime), lessThanOrEqualTo(latestEventTime)));
    }
}

package ucles.weblab.common.tunnel.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ucles.weblab.common.tunnel.client.WebSocketConnectionInstigator.ATTEMPTS_PER_TICK;

/**
 * @since 07/09/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class WebSocketConnectionInstigatorTest {

    @Mock
    private WebSocketContainer webSocketContainer;

    @Mock
    private Session initialSession;

    private WebSocketConnectionInstigator webSocketConnectionInstigator;

    @Before
    public void setUp() throws URISyntaxException {
        TunnelSettings tunnelSettings = new TunnelSettings();

        final URI baseUrl = URI.create("http://fish.jumper/safe/");
        tunnelSettings.setBaseUrl(baseUrl);
        tunnelSettings.setEnabled(true);
        final URI path = URI.create("house.arrest");
        tunnelSettings.setWebSocketPath(path);

        webSocketConnectionInstigator = new WebSocketConnectionInstigator(tunnelSettings, webSocketContainer);
    }

    /**
     * Make an initial connection so that initialSession is associated with the connection instigator, and reset the
     * mocks so that any further interaction has no behaviour defined.
     */
    private void initialiseConnection() {
        try {
            when(webSocketContainer.connectToServer(any(Class.class), eq(URI.create("ws://fish.jumper/safe/house.arrest"))))
                    .thenReturn(initialSession);
            when(initialSession.isOpen()).thenReturn(true);
            webSocketConnectionInstigator.connectAtStartup();
            reset(webSocketContainer, initialSession);
        } catch (DeploymentException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testCheckSessionWithActiveSession() {
        initialiseConnection();
        when(initialSession.isOpen()).thenReturn(true);
        webSocketConnectionInstigator.checkSession();
        verifyZeroInteractions(webSocketContainer);
    }

    @Test
    public void testCheckSessionWithClosedSession() throws IOException, DeploymentException {
        initialiseConnection();
        when(initialSession.isOpen()).thenReturn(false);
        Session newSession = mock(Session.class);
        when(webSocketContainer.connectToServer(any(Class.class), any(URI.class)))
                .thenReturn(newSession);
        when(newSession.isOpen()).thenReturn(true);

        webSocketConnectionInstigator.checkSession();
        verify(webSocketContainer).connectToServer(any(Class.class),
                eq(URI.create("ws://fish.jumper/safe/house.arrest")));
    }

    @Test
    public void testCheckSessionWithNoSession() throws IOException, DeploymentException {
        Session newSession = mock(Session.class);
        when(webSocketContainer.connectToServer(any(Class.class), any(URI.class)))
                .thenReturn(newSession);
        when(newSession.isOpen()).thenReturn(true);

        webSocketConnectionInstigator.checkSession();
        verify(webSocketContainer).connectToServer(any(Class.class),
                eq(URI.create("ws://fish.jumper/safe/house.arrest")));
    }

    @Test
    public void testReconnectRetriesAFewTimes() throws IOException, DeploymentException {
        when(webSocketContainer.connectToServer(any(Class.class), any(URI.class)))
                .thenThrow(DeploymentException.class);
        webSocketConnectionInstigator.reconnect();
        verify(webSocketContainer, times(ATTEMPTS_PER_TICK)).connectToServer(any(Class.class),
                eq(URI.create("ws://fish.jumper/safe/house.arrest")));
    }

    @Test
    public void testFailedReconnectWithCurrentOpenSessionUnsetsSession() throws IOException, DeploymentException {
        initialiseConnection();
        when(initialSession.isOpen()).thenReturn(true);
        webSocketConnectionInstigator.reconnect(); // This failed...

        // ... so calling checkSession will actually try to connect
        Session newSession = mock(Session.class);
        when(webSocketContainer.connectToServer(any(Class.class), any(URI.class)))
                .thenReturn(newSession);
        when(newSession.isOpen()).thenReturn(true);

        webSocketConnectionInstigator.checkSession();
        verify(webSocketContainer, times(ATTEMPTS_PER_TICK + 1)).connectToServer(any(Class.class),
                eq(URI.create("ws://fish.jumper/safe/house.arrest")));
    }
}

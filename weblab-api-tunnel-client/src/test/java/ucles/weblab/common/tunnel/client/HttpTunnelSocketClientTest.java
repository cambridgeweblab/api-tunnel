package ucles.weblab.common.tunnel.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import javax.websocket.DeploymentException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @since 08/09/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class HttpTunnelSocketClientTest {
    private final HttpTunnelSocketClient client = new HttpTunnelSocketClient();
    @Mock
    WebSocketConnectionInstigator webSocketConnectionInstigator;
    @Mock
    ControllerIntrospectingTunnelledQueryHandler controllerIntrospectingTunnelledQueryHandler;
    @Captor
    ArgumentCaptor<TunnelledQuery> tunnelledQueryCaptor;

    @Before
    public void setUp() {
        // The client is not a Spring Bean, so it uses static references to its dependencies...
        WebSocketConnectionInstigator.instance = webSocketConnectionInstigator;
        ControllerIntrospectingTunnelledQueryHandler.instance = controllerIntrospectingTunnelledQueryHandler;
    }

    @Test
    public void testReceiveHandler() throws IOException {
        final String message = "{" +
                "\"correlationId\":\"356f348a-01eb-43ef-ad1c-aa768b2a3102\"," +
                "\"method\":\"POST\"," +
                "\"url\":\"/macadamia/nuts\"," +
                "\"contentType\":\"application/json;charset=utf-8\"," +
                "\"acceptType\":\"application/json\"," +
                "\"body\":\"{}\"" +
                "}";

        TunnelledQuery.Response response = TunnelledQuery.Response.builder()
                .contentType("application/json")
                .correlationId(UUID.fromString("356f348a-01eb-43ef-ad1c-aa768b2a3102"))
                .statusCode(200)
                .body("{\"success\":true}")
                .build();

        when(controllerIntrospectingTunnelledQueryHandler.handleQuery(tunnelledQueryCaptor.capture()))
                .thenReturn(response);
        final String result = client.receiveHandler(message);

        final TunnelledQuery query = tunnelledQueryCaptor.getValue();
        assertEquals(response.correlationId, query.correlationId);
        assertEquals(HttpMethod.POST, query.method);
        assertEquals(URI.create("/macadamia/nuts"), query.url);
        assertEquals(MediaType.APPLICATION_JSON_UTF8, query.contentType);
        assertEquals(MediaType.APPLICATION_JSON, query.acceptType);
        assertEquals("{}", query.body);

        ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode json = objectMapper.readTree(result);
        assertEquals(response.correlationId.toString(), json.get("correlationId").asText());
        assertEquals(response.statusCode, json.get("statusCode").asInt());
        assertEquals(response.contentType, json.get("contentType").asText());
        assertEquals(response.body, json.get("body").asText());
    }

    @Test
    public void testReestablishConnection() throws IOException, DeploymentException {
        client.reestablishConnection();
        verify(webSocketConnectionInstigator).reconnect();
    }
}

package ucles.weblab.common.tunnel.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import javax.websocket.ClientEndpoint;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;

/**
 * @since 16/08/2016
 */
@ClientEndpoint
@Slf4j
public class HttpTunnelSocketClient {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @OnMessage
    public String receiveHandler(String message) throws IOException {
        TunnelledQuery query = objectMapper.readValue(message, TunnelledQuery.class);
        ControllerIntrospectingTunnelledQueryHandler queryHandler = ControllerIntrospectingTunnelledQueryHandler.instance;
        TunnelledQuery.Response response = queryHandler.handleQuery(query);
        return objectMapper.writeValueAsString(response);
    }

    @OnClose
    public void reestablishConnection() {
        log.info("Connection closed. Attempting to auto-reconnect...");
        WebSocketConnectionInstigator.instance.reconnect();
    }

}

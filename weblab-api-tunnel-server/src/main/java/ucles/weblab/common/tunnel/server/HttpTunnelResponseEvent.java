package ucles.weblab.common.tunnel.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * @since 22/08/2016
 */
public class HttpTunnelResponseEvent extends HttpTunnelEvent {
    private static final long serialVersionUID = 3265928854423063292L;
    private final JsonNode payload;

    public HttpTunnelResponseEvent(Object source, WebSocketSession webSocketSession, String name, String payload) throws IOException {
        super(source, webSocketSession, name);
        this.payload = new ObjectMapper().readTree(payload);
    }

    public UUID getCorrelationId() {
        final JsonNode correlationId = payload.get("correlationId");
        return correlationId != null? UUID.fromString(correlationId.asText()) : null;
    }

    public int getStatusCode() {
        return payload.get("statusCode").asInt();
    }

    public String getContentType() {
        return payload.get("contentType").asText();
    }

    public String getBody() {
        return payload.get("body").asText();
    }

    public ClientHttpResponse getClientHttpResponse() {
        return new AbstractClientHttpResponse() {
            @Override
            public int getRawStatusCode() throws IOException {
                return HttpTunnelResponseEvent.this.getStatusCode();
            }

            @Override
            public String getStatusText() throws IOException {
                return HttpStatus.valueOf(HttpTunnelResponseEvent.this.getStatusCode()).getReasonPhrase();
            }

            @Override
            public void close() {

            }

            @Override
            public InputStream getBody() throws IOException {
                return new ByteArrayInputStream(HttpTunnelResponseEvent.this.getBody().getBytes(StandardCharsets.UTF_8));
            }

            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders responseHeaders = new HttpHeaders();
                responseHeaders.setContentType(MediaType.parseMediaType(HttpTunnelResponseEvent.this.getContentType()));
                return responseHeaders;
            }
        };
    }
}

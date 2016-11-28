package ucles.weblab.common.tunnel.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.client.ClientHttpResponse;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @since 26/08/2016
 */
@Slf4j
class WebSocketResponseHandler {
    private final Map<UUID, CompletableFuture<ClientHttpResponse>> pendingResponses = new ConcurrentHashMap<>();

    @EventListener(HttpTunnelResponseEvent.class)
    public void handleTunnelResponse(HttpTunnelResponseEvent e) {
        final UUID correlationId = e.getCorrelationId();
        if (correlationId != null) {
            final CompletableFuture<ClientHttpResponse> response = pendingResponses.remove(correlationId);
            if (response != null) {
                // Handle the response
                response.complete(e.getClientHttpResponse());
            } else {
                log.warn("Received response with unknown correlation ID: " + correlationId + " - " + e.getBody());
            }
        }
    }

    void registerPendingResponse(UUID correlationId, CompletableFuture<ClientHttpResponse> response) {
        pendingResponses.put(correlationId, response);
    }
}

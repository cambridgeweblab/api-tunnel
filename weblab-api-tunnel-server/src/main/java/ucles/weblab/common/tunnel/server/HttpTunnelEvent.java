package ucles.weblab.common.tunnel.server;

import org.springframework.context.ApplicationEvent;
import org.springframework.web.socket.WebSocketSession;

public abstract class HttpTunnelEvent extends ApplicationEvent {
    private final WebSocketSession webSocketSession;
    private final String name;

    HttpTunnelEvent(Object source, WebSocketSession webSocketSession, String name) {
        super(source);
        this.webSocketSession = webSocketSession;
        this.name = name;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public String getName() {
        return name;
    }
}

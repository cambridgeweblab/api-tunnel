package ucles.weblab.common.tunnel.server;

import org.springframework.web.socket.WebSocketSession;

/**
 * @since 22/08/2016
 */
public class HttpTunnelUpEvent extends HttpTunnelEvent {
    private static final long serialVersionUID = 497421135196858577L;

    public HttpTunnelUpEvent(Object source, WebSocketSession webSocketSession, String name) {
        super(source, webSocketSession, name);
    }
}

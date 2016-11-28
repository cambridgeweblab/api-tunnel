package ucles.weblab.common.tunnel.server;

import org.springframework.web.socket.WebSocketSession;

/**
 * @since 22/08/2016
 */
public class HttpTunnelDownEvent extends HttpTunnelEvent {
    private static final long serialVersionUID = -1247544627568636306L;

    public HttpTunnelDownEvent(Object source, WebSocketSession webSocketSession, String name) {
        super(source, webSocketSession, name);
    }
}

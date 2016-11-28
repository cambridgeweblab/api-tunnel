package ucles.weblab.common.tunnel.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.PathMatcher;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @since 02/08/2016
 */
@RequiredArgsConstructor
@Slf4j
public class HttpTunnelSocket extends AbstractWebSocketHandler {
    private static final String NAME_VAR = "name";
    public static final String TUNNEL_PATH = "/internalsystems/{" + NAME_VAR + "}/httpTunnel";

    private final PathMatcher pathMatcher;
    private final ApplicationEventPublisher messageBus;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        final URI uri = session.getUri();
        log.debug("Web socket connection established on: {}", uri);
        final String name = getTunnelName(session);
        log.info("HTTP tunnel '{}' established", name);
        messageBus.publishEvent(new HttpTunnelUpEvent(this, session, name));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        final String name = getTunnelName(session);
        log.debug("HTTP tunnel '{}' response received: {}", name, message.getPayload());
        messageBus.publishEvent(new HttpTunnelResponseEvent(this, session, name, message.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        final String name = getTunnelName(session);
        log.info("HTTP tunnel '{}' closed", name);
        messageBus.publishEvent(new HttpTunnelDownEvent(this, session, name));
    }

    private String getTunnelName(WebSocketSession session) {
        final URI uri = session.getUri();
        String regex = TUNNEL_PATH.replaceAll("\\{[A-za-z0-9_]+\\}", "([^/:]+)").replace("$", "\\$");
        final Matcher matcher = Pattern.compile(regex).matcher(uri.toString());
        if (matcher.find()) {
            final Map<String, String> pathVariables = pathMatcher.extractUriTemplateVariables(TUNNEL_PATH, matcher.group());
            return pathVariables.get(NAME_VAR);
        }
        return null;
    }
}

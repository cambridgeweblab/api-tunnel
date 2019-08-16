package ucles.weblab.common.tunnel.server;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Like an HttpRequest, but executed over a web socket. Use with {@link org.springframework.web.client.RestTemplate}
 * to execute over a web socket rather than over a new HTTP connection.
 *
 * @since 22/08/2016
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class WebSocketHttpRequestFactory implements ClientHttpRequestFactory {
    static int TIMEOUT_SECS = 10; // Package-protected so tests can manipulate it.
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WebSocketSession webSocketSession;
    private final WebSocketResponseHandler webSocketResponseHandler;

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
        return new WebSocketHttpRequest(uri, httpMethod);
    }

    private class WebSocketHttpRequest implements ClientHttpRequest {
        @JsonProperty("correlationId")
        @JsonSerialize(using = ToStringSerializer.class)
        private UUID correlationId = UUID.randomUUID();
        @JsonSerialize(using = ToStringSerializer.class)
        @Getter
        @Setter
        private HttpMethod method;
        @JsonSerialize(using = ToStringSerializer.class)
        private URI url;
        @JsonIgnore
        private final HttpHeaders requestHeaders = new HttpHeaders();
        @JsonIgnore
        private final ByteArrayOutputStream body = new ByteArrayOutputStream(1024);

        WebSocketHttpRequest(URI uri, HttpMethod method) {
            this.url = URI.create(uri.getPath());
            this.method = method;
        }

        @Override
        public HttpMethod getMethod() {
            return method;
        }

        @Override
        public String getMethodValue() {
            return method == null ? null : method.name();
        }

        @JsonIgnore
        public URI getURI() {
            return url;
        }

        public void setURI(URI url) {
            this.url = url;
        }

        @Override
        @JsonIgnore
        public HttpHeaders getHeaders() {
            return requestHeaders;
        }

        @JsonProperty
        public String getContentType() {
            return requestHeaders.getContentType() != null? requestHeaders.getContentType().toString() : null;
        }

        @JsonProperty
        public String getAcceptType() {
            return requestHeaders.getAccept() != null && !requestHeaders.getAccept().isEmpty()? requestHeaders.getAccept().get(0).toString(): null;
        }

        @Override
        @JsonIgnore
        public OutputStream getBody() {
            return body;
        }

        @JsonProperty("body")
        public String getBodyString() {
            return new String(body.toByteArray(), StandardCharsets.UTF_8);
        }

        @Override
        public ClientHttpResponse execute() throws IOException {
            final String query = objectMapper.writeValueAsString(this);
            final CompletableFuture<ClientHttpResponse> response = new CompletableFuture<>();
            webSocketResponseHandler.registerPendingResponse(correlationId, response);
            log.debug("Sending request with correlation ID: " + correlationId);
            webSocketSession.sendMessage(new TextMessage(query));
            try {
                return response.get(TIMEOUT_SECS, TimeUnit.SECONDS);
            } catch (InterruptedException | ExecutionException e) {
                return new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (TimeoutException e) {
                return new ErrorResponse(HttpStatus.REQUEST_TIMEOUT);
            }
        }
    }

    private static class ErrorResponse extends AbstractClientHttpResponse {
        private final HttpStatus statusCode;

        ErrorResponse(HttpStatus statusCode) {
            this.statusCode = statusCode;
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return statusCode.value();
        }

        @Override
        public String getStatusText() throws IOException {
            return statusCode.getReasonPhrase();
        }

        @Override
        public void close() {

        }

        @Override
        public InputStream getBody() throws IOException {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public HttpHeaders getHeaders() {
            return new HttpHeaders();
        }
    }
}

package ucles.weblab.common.tunnel.client;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.FromStringDeserializer;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import static lombok.AccessLevel.NONE;

/**
 * Serialized (JSON) instances of this are received by the {@link HttpTunnelSocketClient}.
 *
 * @since 24/08/2016
 */
@Builder
@Data
@Setter(NONE)
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TunnelledQuery {
    public UUID correlationId;
    public HttpMethod method;
    public URI url;
    @JsonDeserialize(using = MediaTypeDeserializer.class)
    public MediaType contentType;
    @JsonDeserialize(using = MediaTypeDeserializer.class)
    public MediaType acceptType;
    public String body;

    static class MediaTypeDeserializer extends FromStringDeserializer<MediaType> {
        public MediaTypeDeserializer() {
            super(MediaType.class);
        }

        @Override
        protected MediaType _deserialize(String value, DeserializationContext ctxt) throws IOException {
            return MediaType.valueOf(value);
        }
    }

    @Builder
    @Value
    public static class Response {
        public UUID correlationId;
        public int statusCode;
        public String contentType;
        public String body;
    }
}

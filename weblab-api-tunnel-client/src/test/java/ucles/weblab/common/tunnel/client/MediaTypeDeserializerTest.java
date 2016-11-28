package ucles.weblab.common.tunnel.client;

import com.fasterxml.jackson.databind.DeserializationContext;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @since 08/09/2016
 */
public class MediaTypeDeserializerTest {
    private final TunnelledQuery.MediaTypeDeserializer deserializer = new TunnelledQuery.MediaTypeDeserializer();
    private final DeserializationContext context = Mockito.mock(DeserializationContext.class);

    @Test
    public void testDeserialize() throws IOException {
        final MediaType mediaType = deserializer._deserialize(MediaType.TEXT_HTML_VALUE, context);
        assertEquals(MediaType.TEXT_HTML, mediaType);
    }
}

package ucles.weblab.common.tunnel.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Integration test so we can check handling of context refresh, scanning registered controller methods, and
 * resolving handler methods as beans. The unit test does not concern itself with these issues.
 *
 * @since 08/09/2016
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        classes = ControllerIntrospectingTunnelledQueryHandler_IT.WebApp.class)
@TestPropertySource(locations = "classpath:test.properties")
public class ControllerIntrospectingTunnelledQueryHandler_IT {
    @SpringBootApplication
    @Import(WebSocketTunnelConfiguration.class)
    static class WebApp {
    }

    static class Config {
        @Bean
        public TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {
        @RequestMapping(path = "/valedogtorian", method = GET, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
        Map<String, String> testMethod() {
            return Collections.singletonMap("peabody", "sherman");
        }
    }

    @Autowired
    ControllerIntrospectingTunnelledQueryHandler queryHandler;

    @Test
    public void testContextRefreshMakesControllerMethodsAvailable() {
        final TunnelledQuery query = TunnelledQuery.builder()
                .correlationId(UUID.randomUUID())
                .method(HttpMethod.GET)
                .url(URI.create("/valedogtorian")).build();
        final TunnelledQuery.Response response = queryHandler.handleQuery(query);
        assertEquals(query.correlationId, response.correlationId);
        assertEquals(HttpStatus.OK.value(), response.statusCode);
        assertEquals(MediaType.APPLICATION_JSON_UTF8_VALUE, response.contentType);
        assertEquals("{\"peabody\":\"sherman\"}", response.body);
    }
}

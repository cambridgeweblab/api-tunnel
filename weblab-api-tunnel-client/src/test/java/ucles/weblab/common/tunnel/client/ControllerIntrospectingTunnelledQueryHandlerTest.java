package ucles.weblab.common.tunnel.client;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Unit test to check class behaviour more efficiently than the integration test.
 *
 * @since 08/09/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class ControllerIntrospectingTunnelledQueryHandlerTest {
    @Mock
    private RequestMappingHandlerMapping requestMappingHandlerMapping;
    @Mock
    private ConversionService conversionService;
    @Mock
    private MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter;
    @Mock
    private HandlerMethod handlerMethod;

    private PathMatcher pathMatcher = new AntPathMatcher();
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private ControllerIntrospectingTunnelledQueryHandler queryHandler;

    @Before
    public void setUp() {
        when(mappingJackson2HttpMessageConverter.getObjectMapper()).thenReturn(objectMapper);
        when(handlerMethod.createWithResolvedBean()).thenReturn(handlerMethod);
        queryHandler = new ControllerIntrospectingTunnelledQueryHandler(mappingJackson2HttpMessageConverter.getObjectMapper(), requestMappingHandlerMapping, pathMatcher, conversionService);
    }

    private TunnelledQuery registerHandlerMethod(String methodName, Class<?>... parameterTypes) {
        String pattern  = '/' + methodName;
        return registerHandlerMethod(pattern, methodName, parameterTypes);
    }

    private TunnelledQuery registerHandlerMethod(String pattern, String methodName, Class<?>... parameterTypes) {
        setHandlerMethod(methodName, parameterTypes);
        return registerHandlerMapping(pattern).build();
    }

    private TunnelledQuery.TunnelledQueryBuilder registerHandlerMapping(String pattern) {
        RequestMappingInfo requestMappingInfo = new RequestMappingInfo(new PatternsRequestCondition(pattern), null, null, null, null, null, null);

        when(requestMappingHandlerMapping.getHandlerMethods()).thenReturn(singletonMap(requestMappingInfo, handlerMethod));
        queryHandler.scanHandlerMethods();

        return TunnelledQuery.builder()
                .url(URI.create(pattern.replace('{', '_').replace('}', '_')));
    }

    private Method setHandlerMethod(String methodName, Class<?>... parameterTypes) {
        when(handlerMethod.getBean()).thenReturn(TestController.instance);
        try {
            final Method method = TestController.class.getMethod(methodName, parameterTypes);
            when(handlerMethod.getMethod()).thenReturn(method);
            return method;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("MVCPathVariableInspection")
    static class TestController {
        static TestController instance = new TestController();

        public Map<String, String> noArgs() {
            return singletonMap("args", "nope");
        }

        public Map<String, String> oneArg(@PathVariable("widget") String widget) {
            return singletonMap("arg", widget);
        }

        public Map<String, Object> twoArgs(@PathVariable("widget") String widget, @PathVariable("count") Integer count) {
            return new HashMap<String, Object>() {{
                put("arg", widget);
                put("count", count);
            }};
        }

        public Map<String, Integer> oneNumber(@PathVariable("count") Integer count) {
            return singletonMap("arg", count);
        }

        public void throwsException() {
            throw new IllegalArgumentException("illegal, and probably immoral");
        }

        public Map<String, String> bodyText(@RequestBody String pong) {
            return singletonMap("ping", pong);
        }

        public Resource bodyObject(@RequestBody Resource body) {
            final Resource resource = new Resource();
            resource.date = body.date.plusDays(1);
            return resource;
        }
    }

    public static class Resource {
        @JsonProperty
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        LocalDate date;
    }

    @Test
    public void testQueryDoesNotMatchHandler() {
        final TunnelledQuery query = TunnelledQuery.builder()
                .url(URI.create("/doesnotexist"))
                .build();
        assertNull(queryHandler.handleQuery(query));
    }

    @Test
    public void testQueryMatchesNoArgsHandler() {
        final TunnelledQuery query = registerHandlerMethod("noArgs");
        when(handlerMethod.getMethodParameters()).thenReturn(new MethodParameter[0]);
        final TunnelledQuery.Response response = queryHandler.handleQuery(query);
        assertEquals("{\"args\":\"nope\"}", response.body);
    }

    @Test
    public void testQueryHandlerThrowsException() {
        TunnelledQuery query = registerHandlerMethod("throwsException");
        when(handlerMethod.getMethodParameters()).thenReturn(new MethodParameter[0]);
        final TunnelledQuery.Response response = queryHandler.handleQuery(query);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.statusCode);
        assertEquals("{\"message\":\"java.lang.IllegalArgumentException: illegal, and probably immoral\"}", response.body);
    }

    @Test
    public void testObjectMapperThrowsException() throws JsonProcessingException {
        final ObjectMapper spy = spy(objectMapper);
        doThrow(new JsonMappingException("broken mapper")).when(spy).writeValueAsString(any());

        when(mappingJackson2HttpMessageConverter.getObjectMapper()).thenReturn(spy);
        ControllerIntrospectingTunnelledQueryHandler brokenQueryHandler = new ControllerIntrospectingTunnelledQueryHandler(mappingJackson2HttpMessageConverter.getObjectMapper(), requestMappingHandlerMapping, pathMatcher, conversionService);
        final TunnelledQuery query = registerHandlerMethod("noArgs");
        brokenQueryHandler.scanHandlerMethods();

        when(handlerMethod.getMethodParameters()).thenReturn(new MethodParameter[0]);
        final TunnelledQuery.Response response = brokenQueryHandler.handleQuery(query);
        assertEquals("{}", response.body);
    }

    @Test
    public void testQueryWithOnePathVariable() {
        Method method = setHandlerMethod("oneArg", String.class);
        final TunnelledQuery.TunnelledQueryBuilder query = registerHandlerMapping("/oneArg/{widget}");
        when(handlerMethod.getMethodParameters()).thenReturn(new MethodParameter[] {
                new MethodParameter(method, 0)
        });
        query.url(URI.create("/oneArg/yep"));
        final TunnelledQuery.Response response = queryHandler.handleQuery(query.build());
        assertEquals("{\"arg\":\"yep\"}", response.body);
    }

    @Test
    public void testQueryWithUrlEncodedPathVariable() {
        Method method = setHandlerMethod("oneArg", String.class);
        final TunnelledQuery.TunnelledQueryBuilder query = registerHandlerMapping("/oneArg/{widget}");
        when(handlerMethod.getMethodParameters()).thenReturn(new MethodParameter[] {
                new MethodParameter(method, 0)
        });
        query.url(URI.create("/oneArg/%2e"));
        final TunnelledQuery.Response response = queryHandler.handleQuery(query.build());
        assertEquals("{\"arg\":\".\"}", response.body);
    }

    @Test
    public void testQueryWithNonStringPathVariable() {
        Method method = setHandlerMethod("oneNumber", Integer.class);
        final TunnelledQuery.TunnelledQueryBuilder query = registerHandlerMapping("/oneNumber/{count}");
        when(handlerMethod.getMethodParameters()).thenReturn(new MethodParameter[] {
                new MethodParameter(method, 0)
        });
        when(conversionService.convert(eq("435268"), same(Integer.class))).thenReturn(435268);
        query.url(URI.create("/oneNumber/435268"));
        final TunnelledQuery.Response response = queryHandler.handleQuery(query.build());
        assertEquals("{\"arg\":435268}", response.body);
    }

    @Test
    public void testQueryWithTwoPathVariables() {
        Method method = setHandlerMethod("twoArgs", String.class, Integer.class);
        final TunnelledQuery.TunnelledQueryBuilder query = registerHandlerMapping("/oneArg/{widget}/{count}");
        when(handlerMethod.getMethodParameters()).thenReturn(new MethodParameter[] {
                new MethodParameter(method, 0),
                new MethodParameter(method, 1)
        });
        when(conversionService.convert(eq("3243"), same(Integer.class))).thenReturn(3243);
        query.url(URI.create("/oneArg/yep/3243"));
        final TunnelledQuery.Response response = queryHandler.handleQuery(query.build());
        assertEquals("{\"arg\":\"yep\",\"count\":3243}", response.body);
    }

    @Test
    public void testQueryWithBodyText() {
        Method method = setHandlerMethod("bodyText", String.class);
        final TunnelledQuery.TunnelledQueryBuilder query = registerHandlerMapping("/bodyText");
        when(handlerMethod.getMethodParameters()).thenReturn(new MethodParameter[] {
                new MethodParameter(method, 0)
        });
        query.body("pong");
        final TunnelledQuery.Response response = queryHandler.handleQuery(query.build());
        assertEquals("{\"ping\":\"pong\"}", response.body);
    }

    @Test
    public void testQueryWithBodyObject() {
        Method method = setHandlerMethod("bodyObject", Resource.class);
        final TunnelledQuery.TunnelledQueryBuilder query = registerHandlerMapping("/bodyObject");
        when(handlerMethod.getMethodParameters()).thenReturn(new MethodParameter[] {
                new MethodParameter(method, 0)
        });
        query.body("{\"date\":\"2015-04-13\"}");
        final TunnelledQuery.Response response = queryHandler.handleQuery(query.build());
        assertEquals("{\"date\":\"2015-04-14\"}", response.body);
    }

    @Test
    public void testQueryWithInvalidBodyObject() {
        Method method = setHandlerMethod("bodyObject", Resource.class);
        final TunnelledQuery.TunnelledQueryBuilder query = registerHandlerMapping("/bodyObject");
        when(handlerMethod.getMethodParameters()).thenReturn(new MethodParameter[] {
                new MethodParameter(method, 0)
        });
        query.body("{\"date\":\"tomorrow\"}");
        final TunnelledQuery.Response response = queryHandler.handleQuery(query.build());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), response.statusCode);
    }


}

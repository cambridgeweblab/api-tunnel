package ucles.weblab.common.tunnel.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.ConversionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.PathMatcher;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import ucles.weblab.common.webapi.exception.ResourceNotFoundException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

/**
 * @since 25/08/2016
 */
@RequiredArgsConstructor
@Slf4j
public class ControllerIntrospectingTunnelledQueryHandler {
    static ControllerIntrospectingTunnelledQueryHandler instance;

    private final ObjectMapper objectMapper;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final PathMatcher pathMatcher;
    private final ConversionService conversionService;
    private final Map<String, HandlerMethod> handlersByPattern = new HashMap<>();

    @EventListener(ContextRefreshedEvent.class)
    public void scanHandlerMethods() {
        instance = this;
        final Map<RequestMappingInfo, HandlerMethod> handlerMethods = requestMappingHandlerMapping.getHandlerMethods();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
            final HandlerMethod handlerMethod = entry.getValue();
            final Set<String> patterns = entry.getKey().getPatternsCondition().getPatterns();
            for (String pattern : patterns) {
                handlersByPattern.put(pattern, handlerMethod);
            }
        }
    }

    public TunnelledQuery.Response handleQuery(TunnelledQuery query) {
        // TODO: does not yet match on query.method, query.contentType or query.acceptType.
        Optional<String> matchedPattern = handlersByPattern.keySet().stream()
                .filter(pattern -> pathMatcher.match(pattern, query.url.toString()))
                .findFirst();

        if (!matchedPattern.isPresent()) {
          log.warn("Received query on URL {} which did not match any registered pattern.",
              query.url);
            return null;
        }

        HandlerMethod handlerMethod = handlersByPattern.get(matchedPattern.get()).createWithResolvedBean();
        final Map<String, String> templateVariables = pathMatcher.extractUriTemplateVariables(matchedPattern.get(), query.url.toString());
        final TunnelledQuery.Response.ResponseBuilder response = TunnelledQuery.Response.builder();
        try {
            final Object[] arguments = evaluateHandlerMethodArguments(handlerMethod, templateVariables, query.body);

            response.correlationId(query.correlationId);

            final Object result = handlerMethod.getMethod().invoke(handlerMethod.getBean(), arguments);
            response.statusCode(200);
            response.contentType(MediaType.APPLICATION_JSON_UTF8_VALUE); // TODO: introspect @RequestMapping(produces)
            response.body(objectMapper.writeValueAsString(result));
        } catch (ResourceNotFoundException e) {
            buildResponse(response, e, HttpStatus.NOT_FOUND);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | IOException e) {
            buildResponse(response, e, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return response.build();
    }

    private void buildResponse(TunnelledQuery.Response.ResponseBuilder response, Exception e, HttpStatus statusCode) {
        response.statusCode(statusCode.value());
        response.contentType(MediaType.TEXT_PLAIN_VALUE);
        ObjectNode objectNode = objectMapper.createObjectNode();
        Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
        objectNode.put("message", cause.getClass().getName() + ": " + cause.getMessage());
        try {
            response.body(objectMapper.writeValueAsString(objectNode));
        } catch (JsonProcessingException e1) {
            response.body("{}");
        }
    }

    private Object[] evaluateHandlerMethodArguments(HandlerMethod method, Map<String, String> templateVariables, String body) {
        MethodParameter[] parameters = method.getMethodParameters();
        Object[] arguments = new Object[parameters.length];
        Iterator<String> pathVariableValues = templateVariables.values().stream().map((s) -> {
            try {
                return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException var2) {
                throw new RuntimeException(var2);
            }
        }).iterator();

        for (int i = 0; i < parameters.length; ++i) {
            MethodParameter parameter = parameters[i];
            if (parameter.getParameterAnnotation(PathVariable.class) != null) {
                try {
                    if (parameter.getParameterType().isAssignableFrom(String.class)) {
                        arguments[i] = pathVariableValues.next();
                    } else {
                        arguments[i] = this.conversionService.convert(pathVariableValues.next(), parameter.getParameterType());
                    }
                } catch (NoSuchElementException var9) {
                  log.error("No path variable specified for parameter {} [{}]", i, parameter);
                    return null;
                }
            } else if (parameter.getParameterAnnotation(RequestBody.class) != null) {
                if (parameter.getParameterType().isAssignableFrom(String.class)) {
                    arguments[i] = body;
                } else {
                    try {
                        arguments[i] = objectMapper.readValue(body, parameter.getParameterType());
                    } catch (IOException e) {
                      log.error("Could not read @RequestBody into parameter {} [{}] from body: {}",
                          i, parameter, body);
                        return null;
                    }
                }
            } else if (parameter.getParameterAnnotation(AuthenticationPrincipal.class) == null && !Principal.class.isAssignableFrom(parameter.getParameterType()) && !Authentication.class.isAssignableFrom(parameter.getParameterType())) {
              log.error(
                  "Controller method {} parameter {} [{}] is not a @PathVariable or a @RequestBody",
                  method, i, parameter);
                return null;
            } else {
              log.debug("Skipping security parameter {} [{}]", i, parameter);
            }
        }

        return arguments;
    }

}

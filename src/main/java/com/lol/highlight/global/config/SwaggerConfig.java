package com.lol.highlight.global.config;

import com.lol.highlight.global.exception.ErrorCode;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Builder;
import lombok.Getter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
public class SwaggerConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public OpenAPI openAPI() {
        String jwtSchemeName = "JWT Token";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(jwtSchemeName);

        Components components = new Components()
                .addSecuritySchemes(jwtSchemeName, new SecurityScheme()
                        .name(jwtSchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT 토큰을 입력해주세요. (Bearer 제외)"));

        return new OpenAPI()
                .info(new Info()
                        .title("LOL Highlight & Analysis API")
                        .description("AI 기반 LoL 자동 하이라이트 생성 및 경기 분석 시스템 REST API")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("P-Team 4")
                                .email("team4@example.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Server"),
                        new Server().url("https://api.lolhighlight.com").description("Production Server")))
                .addSecurityItem(securityRequirement)
                .components(components);
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            String actualPath = extractActualPath(handlerMethod);

            ApiErrorExamples apiErrorExamples = handlerMethod.getMethodAnnotation(ApiErrorExamples.class);
            if (apiErrorExamples != null) {
                generateErrorCodeResponseExample(operation, apiErrorExamples.value(), actualPath);
            }

            return operation;
        };
    }

    private String extractActualPath(HandlerMethod handlerMethod) {
        try {
            RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();

            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
                if (entry.getValue().equals(handlerMethod)) {
                    RequestMappingInfo info = entry.getKey();

                    var pathPatternsCondition = info.getPathPatternsCondition();
                    if (pathPatternsCondition != null && !pathPatternsCondition.getPatterns().isEmpty()) {
                        return pathPatternsCondition.getPatterns().iterator().next().getPatternString();
                    }

                    var patternsCondition = info.getPatternsCondition();
                    if (patternsCondition != null && !patternsCondition.getPatterns().isEmpty()) {
                        return patternsCondition.getPatterns().iterator().next();
                    }
                }
            }
        } catch (Exception e) {
            // 경로 추출 실패시 기본값 사용
        }

        return "/api/example";
    }

    private void generateErrorCodeResponseExample(Operation operation, ErrorCode[] errorCodes, String actualPath) {
        ApiResponses responses = operation.getResponses();

        Map<Integer, List<ExampleHolder>> statusWithExampleHolders = Arrays.stream(errorCodes)
                .map(errorCode -> ExampleHolder.builder()
                        .example(createErrorExample(errorCode, actualPath))
                        .name(errorCode.name())
                        .httpStatus(errorCode.getStatus().value())
                        .build())
                .collect(Collectors.groupingBy(ExampleHolder::getHttpStatus));

        addExamplesToResponses(responses, statusWithExampleHolders);
    }

    private Example createErrorExample(ErrorCode errorCode, String actualPath) {
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", "2025-11-29T12:00:00.000000");
        errorResponse.put("status", errorCode.getStatus().value());
        errorResponse.put("code", errorCode.getCode());
        errorResponse.put("message", errorCode.getMessage());
        errorResponse.put("path", actualPath);

        Example example = new Example();
        example.description(errorCode.getMessage());
        example.setValue(errorResponse);

        return example;
    }

    private void addExamplesToResponses(ApiResponses responses, Map<Integer, List<ExampleHolder>> statusWithExampleHolders) {
        statusWithExampleHolders.forEach((httpStatus, exampleHolders) -> {
            String statusKey = httpStatus.toString();
            ApiResponse apiResponse = responses.get(statusKey);

            if (apiResponse == null) {
                apiResponse = new ApiResponse();
                apiResponse.setDescription("에러 응답");
                apiResponse.setContent(new Content());
            }

            Content content = apiResponse.getContent();
            MediaType mediaType = content.get("application/json");

            if (mediaType == null) {
                mediaType = new MediaType();
                content.addMediaType("application/json", mediaType);
            }

            Map<String, Example> examples = mediaType.getExamples();
            if (examples == null) {
                examples = new HashMap<>();
                mediaType.setExamples(examples);
            }

            for (ExampleHolder exampleHolder : exampleHolders) {
                examples.put(exampleHolder.getName(), exampleHolder.getExample());
            }

            responses.addApiResponse(statusKey, apiResponse);
        });
    }

    @Getter
    @Builder
    private static class ExampleHolder {
        private final Example example;
        private final String name;
        private final int httpStatus;
    }

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ApiErrorExamples {
        ErrorCode[] value();
    }
}

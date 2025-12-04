package com.lol.highlight.global.config;

import com.lol.highlight.global.common.annotation.ApiErrorExamples;
import com.lol.highlight.global.exception.enums.ErrorCode;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SwaggerOperationCustomizer implements OperationCustomizer {

    @Override
    public Operation customize(Operation operation, HandlerMethod handlerMethod) {
        ApiErrorExamples apiErrorExamples = handlerMethod.getMethodAnnotation(ApiErrorExamples.class);

        if (apiErrorExamples != null) {
            generateErrorCodeResponse(operation, apiErrorExamples.value());
        }

        return operation;
    }

    private void generateErrorCodeResponse(Operation operation, ErrorCode[] errorCodes) {
        ApiResponses responses = operation.getResponses();

        Map<Integer, List<ErrorCode>> statusGrouping = Arrays.stream(errorCodes)
                .collect(Collectors.groupingBy(errorCode -> errorCode.getStatus().value()));

        statusGrouping.forEach((statusCode, errorCodeList) -> {
            Content content = new Content();
            MediaType mediaType = new MediaType();

            Map<String, Example> examples = errorCodeList.stream()
                    .collect(Collectors.toMap(
                            errorCode -> errorCode.name(),
                            errorCode -> {
                                Example example = new Example();
                                example.setValue(String.format(
                                        "{\"status\": %d, \"code\": \"%s\", \"message\": \"%s\"}",
                                        errorCode.getStatus().value(),
                                        errorCode.name(),
                                        errorCode.getMessage()
                                ));
                                return example;
                            }
                    ));

            mediaType.setExamples(examples);
            content.addMediaType("application/json", mediaType);

            ApiResponse apiResponse = new ApiResponse()
                    .description(errorCodeList.get(0).getStatus().getReasonPhrase())
                    .content(content);

            responses.addApiResponse(String.valueOf(statusCode), apiResponse);
        });
    }
}

package com.vansisto.lmbe.common.error.web;

import java.util.Arrays;
import java.util.List;

import com.vansisto.lmbe.common.error.ErrorCode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailSchemaCustomizer implements OpenApiCustomizer {

    private static final String SCHEMA_NAME = ProblemDetail.class.getSimpleName();
    private static final String EXTENSION_MAP_PROPERTY = "properties";

    private static final List<String> ALWAYS_PRESENT = List.of("type", "title", "status", "code");

    @Override
    public void customise(OpenAPI openApi) {
        Schema<?> problemDetail = openApi.getComponents().getSchemas().get(SCHEMA_NAME);
        problemDetail.getProperties().remove(EXTENSION_MAP_PROPERTY);
        problemDetail.addProperty("code", errorCodeSchema());
        problemDetail.addProperty("traceId", new StringSchema());
        ALWAYS_PRESENT.forEach(problemDetail::addRequiredItem);
    }

    private static Schema<String> errorCodeSchema() {
        StringSchema schema = new StringSchema();
        Arrays.stream(ErrorCode.values()).map(Enum::name).forEach(schema::addEnumItemObject);
        return schema;
    }
}

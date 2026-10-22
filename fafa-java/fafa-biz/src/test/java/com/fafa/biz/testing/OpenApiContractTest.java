package com.fafa.biz.testing;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.HashSet;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class OpenApiContractTest extends IdentityHttpSupport {
    @Test void committedContractMatchesImplementedRoutesAndOperationIds() throws Exception {
        var response = mvc.perform(get("/v3/api-docs")).andReturn().getResponse();
        assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(200);
        var runtime = json.readTree(response.getContentAsString()).path("paths");
        Files.writeString(Path.of("target", "app-openapi.json"), response.getContentAsString());
        Path path = Path.of("..", "..", "contracts", "openapi", "app-v1.yaml");
        Object yaml;
        try (var input = Files.newInputStream(path)) {
            yaml = new Yaml(new SafeConstructor(new LoaderOptions())).load(input);
        }
        var contract = json.valueToTree(yaml).path("paths");
        Set<String> operations = new HashSet<>();
        var paths = contract.fields();
        while (paths.hasNext()) {
            var entry = paths.next();
            var methods = entry.getValue().fields();
            while (methods.hasNext()) {
                var method = methods.next();
                String expected = method.getValue().path("operationId").asText();
                assertThat(operations.add(expected)).isTrue();
                assertThat(runtime.path("/v1" + entry.getKey()).path(method.getKey()).path("operationId").asText())
                        .as(entry.getKey()).isEqualTo(expected);
            }
        }
        assertThat(operations).hasSize(27);
        int runtimeCount = 0;
        var runtimePaths = runtime.fields();
        while (runtimePaths.hasNext()) {
            var entry = runtimePaths.next();
            if (entry.getKey().startsWith("/v1/")) runtimeCount += entry.getValue().size();
        }
        assertThat(runtimeCount).isEqualTo(operations.size());
    }
}

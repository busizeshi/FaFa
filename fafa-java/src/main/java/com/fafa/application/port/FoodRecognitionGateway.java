package com.fafa.application.port;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/** Java 到 Python 食品包装识别的窄接口。 */
public interface FoodRecognitionGateway {

    FoodRecognitionResult recognize(FoodRecognitionRequest request);

    record FoodRecognitionRequest(Long taskId, Long userId, Long petId, Long mediaId,
                                  String imageUrl, String promptVersion, String traceId) {
    }

    record FoodRecognitionResult(JsonNode draft, JsonNode rawResponse, String model,
                                 String promptVersion, List<String> warnings) {
    }
}

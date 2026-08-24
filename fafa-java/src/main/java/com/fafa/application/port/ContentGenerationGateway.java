package com.fafa.application.port;

import com.fafa.application.dto.content.ContentGenerationRequest;
import com.fafa.application.dto.content.ContentGenerationResponse;

public interface ContentGenerationGateway {
    ContentGenerationResponse generate(ContentGenerationRequest request);
}

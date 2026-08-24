package com.fafa.application.port;

import com.fafa.application.dto.chat.ChatAgentRequest;
import com.fafa.application.dto.chat.ChatAgentResponse;

/** AI Agent 外部调用端口。 */
public interface ChatAgentGateway {
    ChatAgentResponse chat(ChatAgentRequest request);
}

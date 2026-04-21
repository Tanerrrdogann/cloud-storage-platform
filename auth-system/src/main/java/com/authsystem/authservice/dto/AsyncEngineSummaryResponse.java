package com.authsystem.authservice.dto;

import java.util.List;

public record AsyncEngineSummaryResponse(
        long totalMessages,
        long processedMessages,
        long failedMessages,
        long processingMessages,
        boolean duplicateProtectionEnabled,
        List<AsyncStatusCount> byConsumer,
        List<AsyncStatusCount> byEventType,
        List<ProcessedMessageResponse> recentMessages
) {
}

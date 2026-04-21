package com.authsystem.authservice.dto;

import java.util.List;

public record ModeratorActivitySummaryResponse(
        long totalUsers,
        long activeUsers,
        long suspendedUsers,
        long lockedUsers,
        long moderatorUsers,
        List<UserResponse> recentUsers
) {
}

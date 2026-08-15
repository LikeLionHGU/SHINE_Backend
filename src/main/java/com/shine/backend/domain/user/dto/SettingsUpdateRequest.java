package com.shine.backend.domain.user.dto;

public record SettingsUpdateRequest(
        Boolean cameraAgreed,
        Boolean notificationEnabled
) {}

package com.shine.backend.domain.record.dto;

import java.util.List;

public record RecordListResponse(
        List<RecordItemResponse> items,
        Long nextCursor,
        boolean hasNext
) {}

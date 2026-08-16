package com.shine.backend.domain.record.service;

import com.shine.backend.domain.record.dto.RecordItemResponse;
import com.shine.backend.domain.record.dto.RecordListResponse;
import com.shine.backend.domain.testsheet.entity.ResultStatus;
import com.shine.backend.domain.testsheet.entity.TestSheet;
import com.shine.backend.domain.testsheet.repository.TestResultRepository;
import com.shine.backend.domain.testsheet.repository.TestSheetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RecordService {

    private final TestSheetRepository testSheetRepository;
    private final TestResultRepository testResultRepository;

    @Transactional(readOnly = true)
    public RecordListResponse getRecords(Long userId, Long cursor, int size) {
        // 다음 페이지가 있는지 알기 위해 한 개 더 가져온다
        var pageable = PageRequest.of(0, size + 1);

        List<TestSheet> sheets = cursor == null
                ? testSheetRepository.findByUserIdOrderByIdDesc(userId, pageable)
                : testSheetRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, cursor, pageable);

        boolean hasNext = sheets.size() > size;
        List<TestSheet> page = hasNext ? sheets.subList(0, size) : sheets;

        if (page.isEmpty()) {
            return new RecordListResponse(List.of(), null, false);
        }

        Map<Long, Map<ResultStatus, Long>> countMap = countByStatus(
                page.stream().map(TestSheet::getId).toList());

        List<RecordItemResponse> items = page.stream()
                .map(sheet -> {
                    var counts = countMap.getOrDefault(sheet.getId(), Map.of());
                    return RecordItemResponse.of(sheet,
                            counts.getOrDefault(ResultStatus.DANGER, 0L),
                            counts.getOrDefault(ResultStatus.CAUTION, 0L));
                })
                .toList();

        return new RecordListResponse(
                items,
                hasNext ? page.get(page.size() - 1).getId() : null,
                hasNext);
    }

    /** 검사지마다 따로 세면 N+1이 되므로 한 번에 집계한다 */
    private Map<Long, Map<ResultStatus, Long>> countByStatus(List<Long> sheetIds) {
        Map<Long, Map<ResultStatus, Long>> result = new HashMap<>();

        for (Object[] row : testResultRepository.countStatusBySheetIds(sheetIds)) {
            Long sheetId = (Long) row[0];
            ResultStatus status = (ResultStatus) row[1];
            Long count = (Long) row[2];
            result.computeIfAbsent(sheetId, k -> new HashMap<>()).put(status, count);
        }
        return result;
    }
}

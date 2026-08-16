package com.shine.backend.global.storage;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * 검사지 이미지 저장소.
 *
 * 지금은 서버 로컬 디스크를 쓴다. 나중에 오브젝트 스토리지로 옮기더라도
 * 이 인터페이스만 새로 구현하면 되고 서비스 코드는 그대로다.
 */
public interface FileStorage {

    /** @return 저장 키. DB에는 이 키만 남긴다 */
    String store(MultipartFile file, Long userId);

    List<String> storeAll(List<MultipartFile> files, Long userId);

    InputStream read(String key);

    void delete(String key);

    /** 회원 탈퇴 시 그 사용자의 이미지를 전부 지운다 */
    void deleteAllByUser(Long userId);
}

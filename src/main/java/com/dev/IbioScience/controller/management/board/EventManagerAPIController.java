package com.dev.IbioScience.controller.management.board;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.model.board.event.EventImage;
import com.dev.IbioScience.service.board.event.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/event")
public class EventManagerAPIController {

    private static final String PAGE_CODE = "SITE_EVENT_MANAGER";

    private final EventService eventService;

    /**
     * CKEditor 임시 업로드
     * 생성 화면과 수정 화면에서 같은 URL을 쓰므로
     * CREATE 또는 UPDATE 둘 중 하나라도 있으면 허용합니다.
     */
    @PreAuthorize("@adminMenuFacade.canCreateOrUpdateByPageCode('" + PAGE_CODE + "')")
    @PostMapping("/upload-temp")
    public ResponseEntity<Map<String, Object>> uploadTemp(@RequestParam("upload") MultipartFile upload) {
        EventImage meta = eventService.uploadTempImage(upload);
        return ResponseEntity.ok(Map.of(
                "url", meta.getUrl(),
                "uploaded", true
        ));
    }

    /** 이벤트 삭제 */
    @PreAuthorize("@adminMenuFacade.canDeleteByPageCode('" + PAGE_CODE + "')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
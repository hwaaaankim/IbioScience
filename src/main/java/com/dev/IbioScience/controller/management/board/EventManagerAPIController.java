package com.dev.IbioScience.controller.management.board;

import java.util.Map;

import org.springframework.http.ResponseEntity;
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

    private final EventService eventService;

    /** CKEditor 임시 업로드 */
    @PostMapping("/upload-temp")
    public ResponseEntity<Map<String, Object>> uploadTemp(@RequestParam("upload") MultipartFile upload) {
        EventImage meta = eventService.uploadTempImage(upload);
        return ResponseEntity.ok(Map.of(
                "url", meta.getUrl(),
                "uploaded", true
        ));
    }

    /** 이벤트 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
package com.dev.IbioScience.controller.management.board;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.board.notice.NoticeCreateReq;
import com.dev.IbioScience.dto.board.notice.NoticeUpdateReq;
import com.dev.IbioScience.dto.board.notice.NoticeUploadTempRes;
import com.dev.IbioScience.service.board.notice.NoticeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/manager/notice")
public class NoticeManagerAPIController {

    private final NoticeService noticeService;

    // ✅ CKEditor 임시 업로드
    @PostMapping("/upload-temp")
    public ResponseEntity<NoticeUploadTempRes> uploadTemp(
            @RequestParam("draftKey") String draftKey,
            @RequestParam("upload") MultipartFile upload
    ) {
        return ResponseEntity.ok(noticeService.uploadTempImage(draftKey, upload));
    }

    // ✅ 등록
    @PostMapping
    public ResponseEntity<?> create(@RequestBody NoticeCreateReq req) {
        Long id = noticeService.createNotice(req);
        return ResponseEntity.ok().body(java.util.Map.of("id", id));
    }

    // ✅ 수정
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable("id") Long id, @RequestBody NoticeUpdateReq req) {
        noticeService.updateNotice(id, req);
        return ResponseEntity.ok().body(java.util.Map.of("id", id));
    }

    // ✅ 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") Long id) {
        noticeService.deleteNotice(id);
        return ResponseEntity.ok().body(java.util.Map.of("deleted", true));
    }
}
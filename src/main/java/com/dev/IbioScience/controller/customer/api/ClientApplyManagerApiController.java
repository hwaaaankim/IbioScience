package com.dev.IbioScience.controller.customer.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.customer.auth.ClientApplyDetailDto;
import com.dev.IbioScience.service.auth.admin.client.ClientApplyManagerService;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/root/api/clientApplyManager")
public class ClientApplyManagerApiController {

    private final ClientApplyManagerService clientApplyManagerService;

    @GetMapping("/pending/{memberId}")
    public ResponseEntity<Map<String, Object>> getPendingDetail(@PathVariable Long memberId) {
        ClientApplyDetailDto dto = clientApplyManagerService.getPendingDetail(memberId);
        if (dto == null) {
            return ResponseEntity.ok(apiFail(404, "대기(PENDING) 상태의 신청 데이터를 찾지 못했습니다.", null));
        }
        return ResponseEntity.ok(apiOk("OK", dto));
    }

    @PostMapping("/approve/{memberId}")
    public ResponseEntity<Map<String, Object>> approveOne(@PathVariable Long memberId) {
        int updated = clientApplyManagerService.approveOne(memberId);
        if (updated <= 0) {
            return ResponseEntity.ok(apiFail(409, "승인 처리에 실패했습니다. (이미 처리되었거나 PENDING이 아닙니다)", null));
        }
        return ResponseEntity.ok(apiOk("승인 처리 완료", Map.of("updated", updated)));
    }

    @PostMapping("/approve/bulk")
    public ResponseEntity<Map<String, Object>> approveBulk(@RequestBody ApproveBulkRequest req) {
        List<Long> ids = (req == null) ? null : req.getMemberIds();
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.ok(apiFail(400, "승인할 대상이 없습니다.", null));
        }

        int updated = clientApplyManagerService.approveBulk(ids);
        return ResponseEntity.ok(apiOk("일괄 승인 처리 완료", Map.of("requested", ids.size(), "updated", updated)));
    }

    private Map<String, Object> apiOk(String message, Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("success", true);
        map.put("message", message);
        map.put("data", data);
        return map;
    }

    private Map<String, Object> apiFail(int code, String message, Object data) {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("success", false);
        map.put("message", message);
        map.put("data", data);
        return map;
    }

    @Getter
    @Setter
    public static class ApproveBulkRequest {
        private List<Long> memberIds;
    }
}
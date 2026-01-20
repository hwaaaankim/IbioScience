package com.dev.IbioScience.controller.api.bankda;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.dev.IbioScience.dto.bankda.OrderCheckDTO;
import com.dev.IbioScience.dto.bankda.PaymentCheckDTO;
import com.dev.IbioScience.dto.common.CommonAPIResponse;
import com.dev.IbioScience.service.order.BankdaApiService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/bankda")
@RequiredArgsConstructor
public class BankdaApiController {

    private final BankdaApiService bankdaApiService;

    /**
     * - GET/POST /api/v1/bankda/unCheckedOrderLists
     */
    @RequestMapping(value = "/unCheckedOrderLists", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<CommonAPIResponse<Map<String, Object>>> unCheckedOrderLists() {

        Map<String, Object> data = bankdaApiService.getUnCheckedOrderListsData();
        CommonAPIResponse<Map<String, Object>> body = CommonAPIResponse.ok("정상", data);

        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    /**
     * - POST /api/v1/bankda/orderDetail
     * 요청: { "order_id": 1 }
     */
    @PostMapping("/orderDetail")
    public ResponseEntity<CommonAPIResponse<Map<String, Object>>> orderDetail(
            @RequestBody OrderCheckDTO dto
    ) {

        Map<String, Object> data = bankdaApiService.getOrderDetailData(dto.getOrder_id());

        // 기존 실패 케이스: return_code=415
        if ("415".equals(String.valueOf(data.get("return_code")))) {
            CommonAPIResponse<Map<String, Object>> body = CommonAPIResponse.fail(415, "order_id 오류", data);
            return new ResponseEntity<>(body, HttpStatus.OK);
        }

        CommonAPIResponse<Map<String, Object>> body = CommonAPIResponse.ok("정상", data);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    /**
     * - POST /api/v1/bankda/paymentChecks
     * 요청: { "requests": [ {"order_id":"1"}, {"order_id":"2"} ... ] }
     */
    @PostMapping("/paymentChecks")
    public ResponseEntity<CommonAPIResponse<Map<String, Object>>> paymentChecks(
            @RequestBody PaymentCheckDTO requests
    ) {

        Map<String, Object> data = bankdaApiService.paymentChecksData(
                requests == null ? null : requests.getRequests()
        );

        // 실패 케이스: return_code=415
        if ("415".equals(String.valueOf(data.get("return_code")))) {
            CommonAPIResponse<Map<String, Object>> body = CommonAPIResponse.fail(415, "오류 order_id 체크", data);
            return new ResponseEntity<>(body, HttpStatus.OK);
        }

        CommonAPIResponse<Map<String, Object>> body = CommonAPIResponse.ok("정상", data);
        return new ResponseEntity<>(body, HttpStatus.OK);
    }
}
















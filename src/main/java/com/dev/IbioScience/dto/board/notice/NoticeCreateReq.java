package com.dev.IbioScience.dto.board.notice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoticeCreateReq {
    private String title;
    private String contentHtml;
    private String draftKey;      // temp 이미지 묶음 키
    private Long writerMemberId;  // 선택(로그인 연동 전까지)
    private String writerName;    // 선택(로그인 연동 전까지)
}
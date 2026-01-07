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
public class NoticeUpdateReq {
    private String title;
    private String contentHtml;
    private String draftKey; // 수정 중 새로 업로드된 temp 이미지 묶음 키
}
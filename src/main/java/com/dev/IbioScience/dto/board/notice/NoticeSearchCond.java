package com.dev.IbioScience.dto.board.notice;

import java.time.LocalDate;

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
public class NoticeSearchCond {
    private String title;
    private LocalDate from; // 작성일 from
    private LocalDate to;   // 작성일 to
}
package com.dev.IbioScience.dto.board.event;

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
public class EventSearchCond {

    /** 제목(부분검색) */
    private String title;

    /** 상태: ALL/ONGOING/ENDED */
    private String status;

    /** 작성일 from/to */
    private LocalDate fromDate;
    private LocalDate toDate;

    /** page size */
    private Integer size;
}
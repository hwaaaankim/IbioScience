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
public class EventUpdateReq {
    private String title;
    private LocalDate startDate;
    private LocalDate endDate;
    private String contentHtml;
}
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
public class NoticeUploadTempRes {
    private String url;
    private String originalName;
    private String storedName;
    private long size;
}
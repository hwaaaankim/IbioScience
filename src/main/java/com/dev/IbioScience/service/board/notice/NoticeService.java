package com.dev.IbioScience.service.board.notice;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.board.notice.NoticeCreateReq;
import com.dev.IbioScience.dto.board.notice.NoticeSearchCond;
import com.dev.IbioScience.dto.board.notice.NoticeUpdateReq;
import com.dev.IbioScience.dto.board.notice.NoticeUploadTempRes;

public interface NoticeService {

    Page<?> getNoticePage(NoticeSearchCond cond, Pageable pageable);

    Object getNoticeDetail(Long noticeId);

    Long createNotice(NoticeCreateReq req);

    void updateNotice(Long noticeId, NoticeUpdateReq req);

    void deleteNotice(Long noticeId);

    NoticeUploadTempRes uploadTempImage(String draftKey, MultipartFile file);
}
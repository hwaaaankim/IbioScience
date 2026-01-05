package com.dev.IbioScience.service.board.event;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.dev.IbioScience.dto.board.event.EventCreateReq;
import com.dev.IbioScience.dto.board.event.EventSearchCond;
import com.dev.IbioScience.dto.board.event.EventUpdateReq;
import com.dev.IbioScience.model.board.event.Event;
import com.dev.IbioScience.model.board.event.EventImage;

public interface EventService {

    Page<Event> search(EventSearchCond cond, Pageable pageable);

    Event getOrThrow(Long id);

    Event create(Long writerMemberId, EventCreateReq req, MultipartFile repImage);

    Event update(Long id, EventUpdateReq req, MultipartFile repImage);

    void delete(Long id);

    /** CKEditor 임시 업로드 */
    EventImage uploadTempImage(MultipartFile file);

    /** 조회수 증가 (관리자 상세에서 필요 없으면 안 써도 됨) */
    void increaseViewCount(Long id);
}
package com.dev.IbioScience.repository.board.notice;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.enums.board.NoticeImageStatus;
import com.dev.IbioScience.model.board.notice.NoticeImage;

@Repository
public interface NoticeImageRepository extends JpaRepository<NoticeImage, Long> {

    List<NoticeImage> findByNotice_Id(Long noticeId);

    List<NoticeImage> findByDraftKeyAndImageStatus(String draftKey, NoticeImageStatus status);

    List<NoticeImage> findByNotice_IdAndImageStatus(Long noticeId, NoticeImageStatus status);
}
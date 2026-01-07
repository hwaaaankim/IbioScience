package com.dev.IbioScience.repository.board.notice;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.dev.IbioScience.model.board.notice.Notice;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long>, JpaSpecificationExecutor<Notice> {
}
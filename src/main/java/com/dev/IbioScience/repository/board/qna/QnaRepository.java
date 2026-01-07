package com.dev.IbioScience.repository.board.qna;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.dev.IbioScience.model.board.qna.Qna;

public interface QnaRepository extends JpaRepository<Qna, Long>, JpaSpecificationExecutor<Qna> {
}
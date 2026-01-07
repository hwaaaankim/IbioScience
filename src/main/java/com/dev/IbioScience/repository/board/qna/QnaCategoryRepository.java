package com.dev.IbioScience.repository.board.qna;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.board.qna.QnaCategory;

public interface QnaCategoryRepository extends JpaRepository<QnaCategory, Long> {

    Optional<QnaCategory> findByName(String name);

    List<QnaCategory> findAllByOrderByActiveDescSortOrderAscIdDesc();
}
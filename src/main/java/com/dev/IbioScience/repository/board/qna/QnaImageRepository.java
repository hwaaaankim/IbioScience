package com.dev.IbioScience.repository.board.qna;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dev.IbioScience.model.board.qna.QnaImage;

public interface QnaImageRepository extends JpaRepository<QnaImage, Long> {

    List<QnaImage> findAllByQnaId(Long qnaId);

    Optional<QnaImage> findByImageUrl(String imageUrl);

    void deleteAllByQnaId(Long qnaId);
}
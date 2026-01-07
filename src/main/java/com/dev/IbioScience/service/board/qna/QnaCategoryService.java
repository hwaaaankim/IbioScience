package com.dev.IbioScience.service.board.qna;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.model.board.qna.QnaCategory;
import com.dev.IbioScience.repository.board.qna.QnaCategoryRepository;
import com.dev.IbioScience.repository.board.qna.QnaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QnaCategoryService {

    private final QnaCategoryRepository categoryRepository;
    private final QnaRepository qnaRepository;
    public record CategoryCountRow(QnaCategory category, long qnaCount) {}

    @Transactional(readOnly = true)
    public List<CategoryCountRow> listWithCounts() {
        List<QnaCategory> list = categoryRepository.findAllByOrderByActiveDescSortOrderAscIdDesc();
        List<CategoryCountRow> rows = new ArrayList<>();
        for (QnaCategory c : list) {
            long cnt = qnaRepository.count((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), c.getId())
            );
            rows.add(new CategoryCountRow(c, cnt));
        }
        return rows;
    }
    
    @Transactional(readOnly = true)
    public List<QnaCategory> list() {
        return categoryRepository.findAllByOrderByActiveDescSortOrderAscIdDesc();
    }

    @Transactional
    public QnaCategory create(String name) {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name required");
        categoryRepository.findByName(name.trim()).ifPresent(v -> {
            throw new IllegalStateException("이미 존재하는 카테고리명입니다.");
        });

        QnaCategory c = QnaCategory.builder()
                .name(name.trim())
                .sortOrder(0)
                .active(true)
                .build();

        return categoryRepository.save(c);
    }

    @Transactional
    public QnaCategory update(Long id, String name) {
        if (id == null) throw new IllegalArgumentException("id required");
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("name required");

        QnaCategory c = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("카테고리를 찾을 수 없습니다."));

        String n = name.trim();
        categoryRepository.findByName(n).ifPresent(exist -> {
            if (!exist.getId().equals(id)) throw new IllegalStateException("이미 존재하는 카테고리명입니다.");
        });

        c.setName(n);
        return c;
    }

    @Transactional
    public void delete(Long id) {
        if (id == null) throw new IllegalArgumentException("id required");

        long count = qnaRepository.count((root, query, cb) ->
                cb.equal(root.get("category").get("id"), id)
        );
        if (count > 0) {
            throw new IllegalStateException("등록된 QNA가 있는 카테고리는 삭제할 수 없습니다.");
        }

        categoryRepository.deleteById(id);
    }
}
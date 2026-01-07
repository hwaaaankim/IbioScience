package com.dev.IbioScience.service.board.qna;

import java.io.IOException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dev.IbioScience.dto.board.qna.QnaSpecs;
import com.dev.IbioScience.model.board.qna.Qna;
import com.dev.IbioScience.model.board.qna.QnaCategory;
import com.dev.IbioScience.model.board.qna.QnaImage;
import com.dev.IbioScience.repository.board.qna.QnaCategoryRepository;
import com.dev.IbioScience.repository.board.qna.QnaImageRepository;
import com.dev.IbioScience.repository.board.qna.QnaRepository;
import com.dev.IbioScience.utils.QnaHtmlImageUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QnaService {

	private final QnaRepository qnaRepository;
	private final QnaCategoryRepository categoryRepository;
	private final QnaImageRepository imageRepository;
	private final QnaFileStorageService storageService;

	@Transactional(readOnly = true)
	public Page<Qna> search(String title, LocalDate from, LocalDate to, Pageable pageable) {
		return qnaRepository.findAll(QnaSpecs.search(title, from, to), pageable);
	}

	@Transactional(readOnly = true)
	public Qna getDetail(Long id) {
		return qnaRepository.findById(id).orElseThrow(() -> new IllegalStateException("QNA를 찾을 수 없습니다."));
	}

	@Transactional
	public Long create(Long categoryId, String title, String contentHtml, Long writerMemberId) throws IOException {
		if (categoryId == null)
			throw new IllegalArgumentException("categoryId required");
		if (title == null || title.trim().isEmpty())
			throw new IllegalArgumentException("title required");
		if (contentHtml == null)
			contentHtml = "";

		QnaCategory category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new IllegalStateException("카테고리를 찾을 수 없습니다."));

		Qna qna = Qna.builder().category(category).title(title.trim()).contentHtml(contentHtml).viewCount(0)
				.writerMemberId(writerMemberId).build();

		qnaRepository.save(qna); // id 확보

		// temp -> main 이동 + DB 추적 저장
		syncEditorImages(qna, "", contentHtml);

		return qna.getId();
	}

	@Transactional
	public void update(Long id, Long categoryId, String title, String contentHtml) throws IOException {
		if (id == null)
			throw new IllegalArgumentException("id required");
		if (categoryId == null)
			throw new IllegalArgumentException("categoryId required");
		if (title == null || title.trim().isEmpty())
			throw new IllegalArgumentException("title required");
		if (contentHtml == null)
			contentHtml = "";

		Qna qna = qnaRepository.findById(id).orElseThrow(() -> new IllegalStateException("QNA를 찾을 수 없습니다."));

		QnaCategory category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new IllegalStateException("카테고리를 찾을 수 없습니다."));

		String oldHtml = qna.getContentHtml();

		qna.setCategory(category);
		qna.setTitle(title.trim());
		qna.setContentHtml(contentHtml);

		// 변경이 없어도 syncEditorImages는 안전합니다. (diff가 0이면 이동/삭제 없음)
		syncEditorImages(qna, oldHtml, contentHtml);
	}

	@Transactional
	public void delete(Long id) {
		if (id == null)
			throw new IllegalArgumentException("id required");

		// 이미지 파일 실제 삭제
		List<QnaImage> images = imageRepository.findAllByQnaId(id);
		for (QnaImage img : images) {
			storageService.deleteByUrlIfLocalUpload(img.getImageUrl());
		}

		// Qna 삭제 시 tb_qna_image는 FK cascade delete, 그래도 안전하게 clear 가능
		imageRepository.deleteAll(images);
		qnaRepository.deleteById(id);
	}

	/**
	 * oldHtml vs newHtml 비교하여: - new에 있는 temp 이미지는 main으로 이동 + newHtml url 치환 -
	 * old에는 있었는데 new에는 없는 이미지는 실제 파일 삭제 + DB 삭제 - 최종 newHtml에 남은 이미지들을 QnaImage로
	 * 동기화
	 *
	 * ✅ 중요: - stored_path는 guess 절대 금지 - /upload/** URL은
	 * storageService.toAbsolutePathSafe(url)로 반드시 uploadPath 기반 절대경로로 확정
	 */
	private void syncEditorImages(Qna qna, String oldHtml, String newHtml) throws IOException {
		Set<String> oldUrls = QnaHtmlImageUtil.extractImageSrcUrls(oldHtml);
		Set<String> newUrls = QnaHtmlImageUtil.extractImageSrcUrls(newHtml);

		// 1) 삭제 대상 = old - new
		Set<String> removed = new HashSet<>(oldUrls);
		removed.removeAll(newUrls);

		if (!removed.isEmpty()) {
			// DB/파일 삭제
			for (String url : removed) {
				imageRepository.findByImageUrl(url).ifPresent(imageRepository::delete);
				storageService.deleteByUrlIfLocalUpload(url);
			}
		}

		// 2) temp 이동 대상 = new 중 temp 경로
		String replacedHtml = newHtml;
		Map<String, QnaFileStorageService.MoveResult> movedMap = new HashMap<>();

		for (String url : newUrls) {
			if (url != null && url.startsWith("/upload/qna/temp/")) {
				QnaFileStorageService.MoveResult moved = storageService.moveTempToMain(qna.getId(), url);
				if (moved.moved()) {
					// key는 "원래 temp url" 이어야 치환이 정확합니다.
					movedMap.put(url, moved);
				}
			}
		}

		// 3) HTML 치환 (temp url -> main url)
		if (!movedMap.isEmpty()) {
			for (Map.Entry<String, QnaFileStorageService.MoveResult> e : movedMap.entrySet()) {
				replacedHtml = replacedHtml.replace(e.getKey(), e.getValue().url());
			}
			qna.setContentHtml(replacedHtml);
		}

		// 4) 최종 HTML 기준 이미지 리스트 재추출 후 DB 동기화
		Set<String> finalUrls = QnaHtmlImageUtil.extractImageSrcUrls(qna.getContentHtml());

		// 기존 DB
		List<QnaImage> existing = imageRepository.findAllByQnaId(qna.getId());
		Map<String, QnaImage> existingMap = existing.stream()
				.collect(Collectors.toMap(QnaImage::getImageUrl, v -> v, (a, b) -> a));

		// final에 없으면 삭제(파일은 removed에서 처리되지만, DB가 남을 수 있으니 정리)
		for (QnaImage ex : existing) {
			if (!finalUrls.contains(ex.getImageUrl())) {
				imageRepository.delete(ex);
			}
		}

		// final에 있는데 DB 없으면 insert
		for (String url : finalUrls) {
			if (!existingMap.containsKey(url)) {

				String storedPath;

				// 방금 moved된 경우: movedMap의 value.url()이 main url임
				// 그런데 movedMap은 tempUrl을 key로 가지고 있으니, 여기 url(=finalUrl)은 mainUrl일 가능성이 큼
				// 따라서 movedMap에서 바로 찾을 수 없고, mainUrl은 uploadPath 변환으로 확정하는 게 가장 안전합니다.
				storedPath = storageService.toAbsolutePathSafe(url);

				if (storedPath == null) {
					// /upload/**가 아닌 외부 URL 등: 저장/삭제 추적 대상이 아니므로 빈값
					storedPath = "";
				}

				QnaImage img = QnaImage.builder().qna(qna).imageUrl(url).storedPath(storedPath).build();
				imageRepository.save(img);
			}
		}
	}
}

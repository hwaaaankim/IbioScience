package com.dev.IbioScience.utils;
import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import lombok.AllArgsConstructor;
import lombok.Getter;

public interface FileStorageService {

    @Getter
    @AllArgsConstructor
    class FileSaveResult {
        /**
         * 실제 서버/로컬 파일 시스템상의 전체 경로
         * 예) D:/IbioScience/product/1/review/3/20251209/uuid.jpg
         */
        private final String path;

        /**
         * 브라우저에서 접근 가능한 URL
         * 예) /upload/product/1/review/3/20251209/uuid.jpg
         */
        private final String url;

        /**
         * 저장 파일명 (uuid.xxx)
         */
        private final String fileName;
    }

    /**
     * 리뷰 이미지 저장
     *
     * @param productId 상품 ID
     * @param memberId  회원 ID
     * @param reviewId  리뷰 ID
     * @param file      업로드 파일
     */
    FileSaveResult saveReviewImage(Long productId,
                                   Long memberId,
                                   Long reviewId,
                                   MultipartFile file) throws IOException;

    /**
     * 파일 삭제
     *
     * @param path 실제 파일 시스템 경로
     */
    void delete(String path) throws IOException;
}
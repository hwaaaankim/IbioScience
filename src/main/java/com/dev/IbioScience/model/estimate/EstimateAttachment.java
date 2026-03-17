package com.dev.IbioScience.model.estimate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tb_estimate_attachment")
public class EstimateAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 견적 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estimate_id", nullable = false)
    private Estimate estimate;

    /** 원본 파일명 */
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    /** 저장 파일명 */
    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    /** 서버 저장 경로 */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /** 접근 URL */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /** content type */
    @Column(name = "content_type", length = 100)
    private String contentType;

    /** 파일 크기 */
    @Column(name = "file_size")
    private Long fileSize;

    /** 정렬순서 */
    @Column(name = "sort_order")
    private Integer sortOrder;
}
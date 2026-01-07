package com.dev.IbioScience.model.board.notice;

import com.dev.IbioScience.enums.board.NoticeImageStatus;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tb_notice_image",
       indexes = {
           @Index(name = "ix_notice_image_notice_id", columnList = "notice_id"),
           @Index(name = "ix_notice_image_draft_key", columnList = "draft_key"),
           @Index(name = "ix_notice_image_status", columnList = "image_status")
       })
public class NoticeImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK (nullable: 임시업로드 단계에서는 notice_id 없이 저장)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id")
    private Notice notice;

    @Column(name = "draft_key", length = 64)
    private String draftKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_status", nullable = false, length = 20)
    private NoticeImageStatus imageStatus;

    @Column(name = "url", nullable = false, length = 600)
    private String url;

    @Column(name = "stored_rel_path", nullable = false, length = 600)
    private String storedRelPath;

    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    @Column(name = "original_name", length = 255)
    private String originalName;

    @Column(name = "file_size", nullable = false)
    private long fileSize;
}
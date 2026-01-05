package com.dev.IbioScience.model.board.event;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tb_event_image", indexes = {
        @Index(name = "ix_event_image_event", columnList = "event_id"),
        @Index(name = "ix_event_image_kind", columnList = "kind"),
        @Index(name = "ix_event_image_url", columnList = "url")
})
public class EventImage {

    public enum Kind {
        CONTENT,
        REPRESENTATIVE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이벤트 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_event_image_event"))
    private Event event;

    /** 이미지 종류 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Kind kind;

    /** 원본명 */
    @Column(name = "original_name", length = 255)
    private String originalName;

    /** 저장명(랜덤16+확장자) */
    @Column(name = "stored_name", nullable = false, length = 80)
    private String storedName;

    /** 상대경로 (event/temp/... or event/main/...) */
    @Column(name = "rel_path", nullable = false, length = 500)
    private String relPath;

    /** URL (/upload/...) */
    @Column(name = "url", nullable = false, length = 700)
    private String url;

    /** bytes */
    @Column(name = "size")
    private Long size;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
package com.dev.IbioScience.model.board.event;

import java.time.LocalDate;

import com.dev.IbioScience.model.auth.Member;
import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
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
@Table(name = "tb_event", indexes = {
        @Index(name = "ix_event_writer", columnList = "writer_member_id"),
        @Index(name = "ix_event_created_at", columnList = "created_at"),
        @Index(name = "ix_event_start_end", columnList = "start_date,end_date")
})
public class Event extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 제목 */
    @Column(nullable = false, length = 200)
    private String title;

    /** 작성 멤버 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "writer_member_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_event_writer_member"))
    private Member writer;

    /** 에디터 본문 HTML */
    @Lob
    @Column(name = "content_html")
    private String contentHtml;

    /** 조회수 */
    @Column(name = "view_count", nullable = false)
    private Long viewCount;

    /** 시작일 */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** 종료일 */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /* =========================
       대표 이미지 메타
    ========================= */

    @Column(name = "rep_image_original_name", length = 255)
    private String repImageOriginalName;

    @Column(name = "rep_image_stored_name", length = 80)
    private String repImageStoredName;

    @Column(name = "rep_image_rel_path", length = 500)
    private String repImageRelPath;

    @Column(name = "rep_image_url", length = 700)
    private String repImageUrl;

    @Column(name = "rep_image_size")
    private Long repImageSize;

    @PrePersist
    void prePersist() {
        if (viewCount == null) viewCount = 0L;
    }
}
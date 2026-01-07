package com.dev.IbioScience.model.board.notice;

import com.dev.IbioScience.model.auth.embedded.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
@Table(name = "tb_notice")
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Lob
    @Column(name = "content_html", nullable = false)
    private String contentHtml;

    @Column(name = "writer_member_id")
    private Long writerMemberId;

    @Column(name = "writer_name", length = 100)
    private String writerName;

    @Column(name = "view_count", nullable = false)
    private int viewCount;
}
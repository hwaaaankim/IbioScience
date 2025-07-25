package com.dev.IbioScience.model.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

//브랜드 엔티티
@Data
@Entity
@Table(name = "tb_brand")
public class Brand {
	
    // 브랜드 ID, PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 브랜드명
    @Column(nullable = false, length = 100)
    private String name;

    // 브랜드 이미지 경로 (서버 저장용 경로)
    @Column(length = 500)
    private String imagePath;

    // 브랜드 이미지 URL(로드, 외부 접근 URL)
    @Column(length = 500)
    private String imageRoad;
}


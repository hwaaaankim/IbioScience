package com.dev.IbioScience.model.auth;

import java.time.LocalDateTime;

import com.dev.IbioScience.model.product.enums.DealerGrade;
import com.dev.IbioScience.model.product.enums.MemberRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "tb_member")
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username; // 아이디(이메일/고유값)

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 30)
    private String name;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    @Enumerated(EnumType.STRING)
    private DealerGrade dealerGrade; // 딜러일 경우 등급

    @Column(length = 15)
    private String businessNumber; // 사업자번호

    @Column(length = 50)
    private String companyName;

    @Column(length = 50)
    private String ceoName;

    @Column(nullable = false)
    private boolean businessVerified;

    @Column(nullable = false)
    private boolean enabled;

    @Column
    private LocalDateTime joinedAt;

    @Column
    private LocalDateTime leavedAt;

    @Column
    private LocalDateTime updatedAt;

}

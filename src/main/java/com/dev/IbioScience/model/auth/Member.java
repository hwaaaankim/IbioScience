package com.dev.IbioScience.model.auth;

import java.io.Serializable;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name="tb_member")
public class Member implements Serializable{

	private static final long serialVersionUID = 1L;
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String name;
    private String phone;
    private String email;
    private String telephone;
    
    @Enumerated(EnumType.STRING)
    private MemberRole role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JsonBackReference
    private Company company; // 고객사 소속일 경우

    private boolean enabled = true;
    private LocalDateTime createdAt = LocalDateTime.now(); // 회원가입일
    private LocalDateTime updatedAt; // 최근 회원정보 수정일
    private LocalDateTime lastLoginAt; // 마지막 로그인 시각
	
}

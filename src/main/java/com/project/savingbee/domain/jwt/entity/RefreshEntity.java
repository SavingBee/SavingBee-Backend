package com.project.savingbee.domain.jwt.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "jwt_refresh_entity")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)//발급한 계정
    private String username;

    @Column(name = "refresh", nullable = false, length = 512)//리프레쉬 토큰 저장하는 필드
    private String refresh;

    @CreatedDate
    @Column(name = "created_date", updatable = false)//리프레쉬 토큰을 발급했을때의 생성시간
    private LocalDateTime createdDate;
}

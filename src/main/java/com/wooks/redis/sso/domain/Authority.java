package com.wooks.redis.sso.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.springframework.security.core.GrantedAuthority;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Authority implements GrantedAuthority {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "AUTHORITY_ID")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEMBER_ID")
    private Member member;

    private String role;

    public static Authority ofUser(Member member) {
        return Authority.builder()
                .role("ROLE_USER")
                .member(member)
                .build();
    }

    public static Authority ofAdmin(Member member) {
        return Authority.builder()
                .role("ROLE_ADMIN")
                .member(member)
                .build();
    }

    @Override
    public String getAuthority() {
        return role;
    }
}

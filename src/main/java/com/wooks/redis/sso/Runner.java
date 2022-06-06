package com.wooks.redis.sso;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.wooks.redis.sso.domain.dto.JoinDto;
import com.wooks.redis.sso.service.MemberService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@Transactional
public class Runner implements CommandLineRunner {

    private final MemberService memberService;

    @Override
    public void run(String... args) throws Exception {
        JoinDto testJoin = JoinDto.builder()
                .email("test@test.com")
                .password("1234")
                .nickname("test")
                .build();
        memberService.join(testJoin);

        JoinDto adminJoin = JoinDto.builder()
                .email("admin@admin.com")
                .password("1234")
                .nickname("admin")
                .build();
        memberService.joinAdmin(adminJoin);
    }

}

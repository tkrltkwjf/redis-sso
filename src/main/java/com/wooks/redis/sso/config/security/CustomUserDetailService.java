package com.wooks.redis.sso.config.security;

import java.util.NoSuchElementException;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.wooks.redis.sso.config.cache.CacheKey;
import com.wooks.redis.sso.domain.Member;
import com.wooks.redis.sso.domain.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

	    private final MemberRepository memberRepository;

	    @Override
	    @Cacheable(value = CacheKey.USER, key = "#username", unless = "#result == null")
	    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
	        Member member = memberRepository.findByUsernameWithAuthority(username).orElseThrow(() -> new NoSuchElementException("없는 회원입니다."));
	        return CustomUserDetails.of(member);
	    }
	}
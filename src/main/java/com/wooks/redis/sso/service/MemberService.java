package com.wooks.redis.sso.service;

import java.util.NoSuchElementException;

import javax.transaction.Transactional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.wooks.redis.sso.config.cache.CacheKey;
import com.wooks.redis.sso.config.jwt.JwtExpirationEnums;
import com.wooks.redis.sso.domain.LogoutAccessToken;
import com.wooks.redis.sso.domain.LogoutAccessTokenRedisRepository;
import com.wooks.redis.sso.domain.Member;
import com.wooks.redis.sso.domain.MemberRepository;
import com.wooks.redis.sso.domain.RefreshToken;
import com.wooks.redis.sso.domain.RefreshTokenRedisRepository;
import com.wooks.redis.sso.domain.dto.JoinDto;
import com.wooks.redis.sso.domain.dto.LoginDto;
import com.wooks.redis.sso.domain.dto.MemberInfo;
import com.wooks.redis.sso.domain.dto.TokenDto;
import com.wooks.redis.sso.util.JwtTokenUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

	  private final MemberRepository memberRepository;
	    private final PasswordEncoder passwordEncoder;
	    private final RefreshTokenRedisRepository refreshTokenRedisRepository;
	    private final LogoutAccessTokenRedisRepository logoutAccessTokenRedisRepository;
	    private final JwtTokenUtil jwtTokenUtil;

	    public void join(JoinDto joinDto) {
	        joinDto.setPassword(passwordEncoder.encode(joinDto.getPassword()));
	        memberRepository.save(Member.ofUser(joinDto));
	    }

	    public void joinAdmin(JoinDto joinDto) {
	        joinDto.setPassword(passwordEncoder.encode(joinDto.getPassword()));
	        memberRepository.save(Member.ofAdmin(joinDto));
	    }

	    // 1
	    public TokenDto login(LoginDto loginDto) {
	        Member member = memberRepository.findByEmail(loginDto.getEmail()).orElseThrow(() -> new NoSuchElementException("????????? ????????????."));
	        checkPassword(loginDto.getPassword(), member.getPassword());

	        String username = member.getUsername();
	        String accessToken = jwtTokenUtil.generateAccessToken(username);
	        RefreshToken refreshToken = saveRefreshToken(username);
	        return TokenDto.of(accessToken, refreshToken.getRefreshToken());
	    }

	    private void checkPassword(String rawPassword, String findMemberPassword) {
	        if (!passwordEncoder.matches(rawPassword, findMemberPassword)) {
	            throw new IllegalArgumentException("??????????????? ?????? ????????????.");
	        }
	    }

	    private RefreshToken saveRefreshToken(String username) {
	        return refreshTokenRedisRepository.save(RefreshToken.createRefreshToken(username,
	                jwtTokenUtil.generateRefreshToken(username), JwtExpirationEnums.REFRESH_TOKEN_EXPIRATION_TIME.getValue()));
	    }

	    // 2
	    public MemberInfo getMemberInfo(String email) {
	        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new NoSuchElementException("????????? ????????????."));
	        if (!member.getUsername().equals(getCurrentUsername())) {
	            throw new IllegalArgumentException("?????? ????????? ???????????? ????????????.");
	        }
	        return MemberInfo.builder()
	                .username(member.getUsername())
	                .email(member.getEmail())
	                .build();
	    }

	    // 4
	    @CacheEvict(value = CacheKey.USER, key = "#username")
	    public void logout(TokenDto tokenDto, String username) {
	        String accessToken = resolveToken(tokenDto.getAccessToken());
	        long remainMilliSeconds = jwtTokenUtil.getRemainMilliSeconds(accessToken);
	        refreshTokenRedisRepository.deleteById(username);
	        logoutAccessTokenRedisRepository.save(LogoutAccessToken.of(accessToken, username, remainMilliSeconds));
	    }

	    private String resolveToken(String token) {
	        return token.substring(7);
	    }

	    // 3
	    public TokenDto reissue(String refreshToken) {
	        refreshToken = resolveToken(refreshToken);
	        String username = getCurrentUsername();
	        RefreshToken redisRefreshToken = refreshTokenRedisRepository.findById(username).orElseThrow(NoSuchElementException::new);

	        if (refreshToken.equals(redisRefreshToken.getRefreshToken())) {
	            return reissueRefreshToken(refreshToken, username);
	        }
	        throw new IllegalArgumentException("????????? ???????????? ????????????.");
	    }

	    private String getCurrentUsername() {
	        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
	        UserDetails principal = (UserDetails) authentication.getPrincipal();
	        return principal.getUsername();
	    }

	    private TokenDto reissueRefreshToken(String refreshToken, String username) {
	        if (lessThanReissueExpirationTimesLeft(refreshToken)) {
	            String accessToken = jwtTokenUtil.generateAccessToken(username);
	            return TokenDto.of(accessToken, saveRefreshToken(username).getRefreshToken());
	        }
	        return TokenDto.of(jwtTokenUtil.generateAccessToken(username), refreshToken);
	    }

	    private boolean lessThanReissueExpirationTimesLeft(String refreshToken) {
	        return jwtTokenUtil.getRemainMilliSeconds(refreshToken) < JwtExpirationEnums.REISSUE_EXPIRATION_TIME.getValue();
	    }
}
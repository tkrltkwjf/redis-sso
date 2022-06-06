package com.wooks.redis.sso.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;




public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByEmail(String email);

	Optional<Member> findByUsername(String username);

    @Query("select m from Member m join fetch m.authorities a where m.username = :username")
    Optional<Member> findByUsernameWithAuthority(String username);
}

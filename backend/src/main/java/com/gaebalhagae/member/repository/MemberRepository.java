package com.gaebalhagae.member.repository;

import com.gaebalhagae.member.domain.Member;
import com.gaebalhagae.member.domain.Provider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByProviderAndProviderId(Provider provider, String providerId);
}

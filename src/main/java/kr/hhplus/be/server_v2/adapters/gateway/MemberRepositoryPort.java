package kr.hhplus.be.server_v2.adapters.gateway;

import kr.hhplus.be.server_v2.entity.member.Member;

import java.util.Optional;

public interface MemberRepositoryPort {

    Optional<Member> findByMemberId(String memberId);
}

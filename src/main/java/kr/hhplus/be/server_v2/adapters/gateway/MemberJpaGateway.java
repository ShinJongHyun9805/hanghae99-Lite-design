package kr.hhplus.be.server_v2.adapters.gateway;

import kr.hhplus.be.server_v2.entity.member.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;


@Component
@RequiredArgsConstructor
public class MemberJpaGateway implements MemberRepositoryPort {

    private final MemberRepository repository;

    @Override
    public Optional<Member> findByMemberId(String memberId) {
        return repository.findByMemberId(memberId);
    }
}

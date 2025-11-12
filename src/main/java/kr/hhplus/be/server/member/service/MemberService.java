package kr.hhplus.be.server.member.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exception.DuplicateMemberException;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.dto.MemberJoinRequestDto;
import kr.hhplus.be.server.member.dto.MemberJoinResponseDto;
import kr.hhplus.be.server.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;


    public ResponseEntity<MemberJoinResponseDto> join(MemberJoinRequestDto requestDto) {

        if (memberRepository.existsByMemberId(requestDto.getMemberId())) {
            throw new DuplicateMemberException("이미 가입된 회원입니다.");
        }

        Member member = new Member().joinMember(requestDto.getMemberId(), requestDto.getName());
        Member save = memberRepository.save(member);

        return ResponseEntity.ok(new MemberJoinResponseDto(save.getMemberId(), save.getMemberName()));
    }
}

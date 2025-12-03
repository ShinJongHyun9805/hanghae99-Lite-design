package kr.hhplus.be.server.member.service;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.config.jwt.JwtTokenProvider;
import kr.hhplus.be.server.common.exception.DuplicateMemberException;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.domain.Role;
import kr.hhplus.be.server.member.dto.MemberAuthDto;
import kr.hhplus.be.server.member.dto.MemberAuthDto.MeResponse;
import kr.hhplus.be.server.member.dto.MemberAuthDto.SignUpRequest;
import kr.hhplus.be.server.member.dto.MemberAuthDto.TokenResponse;
import kr.hhplus.be.server.member.dto.MemberJoinRequestDto;
import kr.hhplus.be.server.member.dto.MemberJoinResponseDto;
import kr.hhplus.be.server.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public ResponseEntity<MeResponse> singUp(SignUpRequest requestDto) {

        if (memberRepository.existsByMemberId(requestDto.memberId())) {
            throw new DuplicateMemberException("이미 가입된 회원입니다.");
        }

        Member createMember = Member.builder()
                .memberId(requestDto.memberId())
                .password(passwordEncoder.encode(requestDto.password()))
                .memberName(requestDto.name())
                .roles(Set.of(Role.MEMBER))
                .build();

        memberRepository.save(createMember);

        return ResponseEntity.ok(new MeResponse(createMember.getId(),createMember.getMemberId(), createMember.getMemberName()));
    }

    public TokenResponse login(MemberAuthDto.LoginRequest requestDto) {

        Authentication auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(requestDto.memberId(), requestDto.password()));

        Set<String> roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toSet());

        var token = tokenProvider.createToken(auth.getName(), roles);

        return TokenResponse.bearer(token);
    }

    public MeResponse me(String memberId) {
        var member = memberRepository.findByMemberId(memberId).orElseThrow();
        return new MeResponse(member.getId(), member.getMemberId(), member.getMemberName());
    }
}

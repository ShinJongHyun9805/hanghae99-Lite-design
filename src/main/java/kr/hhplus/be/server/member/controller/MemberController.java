package kr.hhplus.be.server.member.controller;

import kr.hhplus.be.server.member.docs.MemberApiDocs;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.dto.MemberJoinRequestDto;
import kr.hhplus.be.server.member.dto.MemberJoinResponseDto;
import kr.hhplus.be.server.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController implements MemberApiDocs {

    private final MemberService memberService;


    @Override
    @PostMapping("/join")
    public ResponseEntity<String> join(@Validated @RequestBody MemberJoinRequestDto requestDto) {

        ResponseEntity<MemberJoinResponseDto> member = memberService.join(requestDto);

        return ResponseEntity.ok(member.getBody().getName() + " 님, 회원 가입이 완료되었습니다.");
    }
}

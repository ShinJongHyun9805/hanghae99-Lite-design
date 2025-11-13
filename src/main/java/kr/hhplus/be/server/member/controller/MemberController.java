package kr.hhplus.be.server.member.controller;

import jakarta.validation.Valid;
import kr.hhplus.be.server.member.dto.MemberAuthDto.LoginRequest;
import kr.hhplus.be.server.member.dto.MemberAuthDto.MeResponse;
import kr.hhplus.be.server.member.dto.MemberAuthDto.SignUpRequest;
import kr.hhplus.be.server.member.dto.MemberAuthDto.TokenResponse;
import kr.hhplus.be.server.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/sign-up")
    public ResponseEntity<String> singUp(@Valid @RequestBody SignUpRequest requestDto) {

        ResponseEntity<MeResponse> member = memberService.singUp(requestDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(member.getBody().name() + " 님, 회원 가입이 완료되었습니다.");
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest req) {
        return ResponseEntity.ok(memberService.login(req));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(memberService.me(principal.getUsername()));
    }
}

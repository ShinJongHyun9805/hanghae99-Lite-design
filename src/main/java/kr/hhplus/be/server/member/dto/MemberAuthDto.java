package kr.hhplus.be.server.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MemberAuthDto {

    // 회원 가입 요청 DTO
    public record SignUpRequest(
            @NotBlank String memberId,
            @NotBlank @Size(min = 8, max = 64) String password,
            @NotBlank @Size(min = 1, max = 50) String name
    ) {}

    // 로그인 요청 DTO
    public record LoginRequest(
            @NotBlank String memberId,
            @NotBlank String password
    ) {}

    // 토큰 응답 DTO
    public record TokenResponse(
            String accessToken,
            String tokenType
    ) {
        // 편의 메서드
        public static TokenResponse bearer(String token) {
            return new TokenResponse(token, "Bearer");
        }
    }

    // 사용자 정보 응답 DTO
    public record MeResponse(
            Long id,
            String memberId,
            String name
    ) {}
}

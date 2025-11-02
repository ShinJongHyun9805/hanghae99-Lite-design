package kr.hhplus.be.server.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "회원가입 응답 DTO")
public class MemberJoinResponseDto {

    @Schema(description = "회원 ID", example = "parkjh")
    private String memberId;

    @Schema(description = "회원 이름", example = "박종현")
    private String name;

}
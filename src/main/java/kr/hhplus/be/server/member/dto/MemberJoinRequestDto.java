package kr.hhplus.be.server.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@NoArgsConstructor
@Schema(description = "회원가입 요청 DTO")
public class MemberJoinRequestDto {

    @Schema(description = "회원 ID", example = "parkjh", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "회원 ID는 필수 입력 값입니다.")
    private String memberId;

    @Schema(description = "회원 이름", example = "박종현", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "회원 이름은 필수 입력 값입니다.")
    private String name;
}

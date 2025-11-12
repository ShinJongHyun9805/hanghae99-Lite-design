package kr.hhplus.be.server.member.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import kr.hhplus.be.server.member.dto.MemberJoinRequestDto;
import kr.hhplus.be.server.member.dto.MemberJoinResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

public interface MemberApiDocs {

    @Operation(summary = "회원 가입", description = "회원 ID와 이름을 입력받아 새로운 회원을 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "회원가입 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MemberJoinResponseDto.class),
                            examples = @ExampleObject(value = """
                                    "박종현 님, 회원 가입이 완료되었습니다."
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "회원 ID 누락",
                                            value = """
                                                    "회원 ID는 필수 입력 값입니다."
                                                    """),
                                    @ExampleObject(name = "회원 이름 누락",
                                            value = """
                                                    "회원 이름은 필수 입력 값입니다."
                                                    """),
                                    @ExampleObject(name = "중복 회원",
                                            value = """
                                                    "이미 가입된 회원입니다."
                                                    """)
                            }))
    })
    @PostMapping("/join")
    ResponseEntity<String> join(@Validated @RequestBody MemberJoinRequestDto requestDto);
}
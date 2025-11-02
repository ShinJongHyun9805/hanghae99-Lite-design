package kr.hhplus.be.server.point.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import kr.hhplus.be.server.point.domain.Point;
import kr.hhplus.be.server.point.dto.PointChargeRequestDto;
import kr.hhplus.be.server.point.dto.PointChargeResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.validation.annotation.Validated;

public interface PointApiDocs {

    @Operation(summary = "포인트 충전", description = "회원 ID와 금액을 받아 포인트를 충전합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "충전 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PointChargeResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "memberId": "park",
                                      "pointAmt": 10
                                    }
                                    """))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "회원 ID 누락",
                                            value = """
                                                    "회원 ID는 필수 입력 값입니다."
                                                    """),
                                    @ExampleObject(name = "충전 금액 오류",
                                            value = """
                                                    "충전 금액은 1원 이상이어야 합니다."
                                                    """)
                            }))
    })
    @PostMapping("/charge")
    ResponseEntity<PointChargeResponseDto> chargePoint(@Validated @RequestBody PointChargeRequestDto requestDto);

    @Operation(summary = "포인트 조회", description = "해당 회원의 포인트를 조회 합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PointChargeResponseDto.class),
                            examples = @ExampleObject(value = """
                                    {
                                      "id": 1,
                                      "memberId": "park",
                                      "pointAmt": 1000
                                    }
                                    """))),
            @ApiResponse(responseCode = "401", description = "잘못된 요청",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "유효하지 않은 회원인 경우",
                                            value = """
                                                    "유효하지 않은 회원입니다."
                                                    """)
                            }))
    })
    @GetMapping("/{memberId}")
    ResponseEntity<PointChargeResponseDto> getPoint(@PathVariable("memberId") String memberId);
}
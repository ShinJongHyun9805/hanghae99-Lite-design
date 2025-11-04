package kr.hhplus.be.server.payment.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import kr.hhplus.be.server.concert.dto.ConcertListResponseDto;
import kr.hhplus.be.server.concert.dto.ConcertReserveRequestDto;
import kr.hhplus.be.server.concert.dto.ConcertScheduleListResponseDto;
import kr.hhplus.be.server.payment.dto.PaymentPendingRequestDto;
import kr.hhplus.be.server.payment.dto.PaymentPendingResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface PaymentApiDocs {

    @Operation(summary = "결제 내역 조회", description = "해당 유저의 결제 내역을 조회한다.")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "결제 내역 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = PaymentPendingResponseDto.class),
                            examples = {
                                    @ExampleObject(name = "결제 대기", value = """
                                            { 
                                                "paymentId": 1,
                                                "memberId": 1,
                                                "memberName": "jonghyun",
                                                "paymentStatusNm": "결제 대기",
                                                "title": "wanna see you dance again",
                                                "price": 100000,
                                                "startDate": "2025-12-03",
                                                "seatNumber": 15,
                                                "paymentDate": "",
                                                "cancelReason" : ""
                                            }
                                        """),
                                    @ExampleObject(name = "결제 완료", value = """
                                            {
                                                "paymentId": 2,
                                                "memberId": 1,
                                                "memberName": "jonghyun",
                                                "paymentStatusNm": "결제 완료",
                                                "title": "Still Life",
                                                "price": 100000,
                                                "startDate": "2025-12-10",
                                                "seatNumber": 20,
                                                "paymentDate": "2025-11-03",
                                                "cancelReason" : ""
                                             }
                                         """),
                                    @ExampleObject(name = "결제 취소", value = """
                                            {
                                                "paymentId": 3,
                                                "memberId": 1,
                                                "memberName": "jonghyun",
                                                "paymentStatusNm": "결제 취소",
                                                "title": "Still Life",
                                                "price": 100000,
                                                "startDate": "2025-12-10",
                                                "seatNumber": 20,
                                                "paymentDate": "",
                                                "cancelReason" : "결제 시간 초과로 인한 자동 취소"
                                            }
                                        """)
                            }
                    )
            )
    })
    @GetMapping("/history/{memberId}")
    ResponseEntity<List<PaymentPendingResponseDto>> getPaymentHistory(@PathVariable("memberId") String memberId);


    @Operation(summary = "결제 요청", description = "결제한다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "결제 완료",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    "결제 완료"
                                    """))),
            @ApiResponse(responseCode = "400", description = "결제 실패",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "결제 ID 누락",
                                            value = """
                                                    "paymentId는 필수 입력 값입니다."
                                                    """),
                                    @ExampleObject(name = "회원 ID 누락",
                                            value = """
                                                    "memberId는 필수 입력 값입니다."
                                                    """),
                                    @ExampleObject(name = "포인트 부족",
                                            value = """
                                                    "결제 금액이 부족합니다. 충전 포인트를 확인해주세요."
                                                    """),
                                    @ExampleObject(name = "이미 취소된 결제",
                                            value = """
                                                    "이미 취소된 결제입니다. 새로고침 해주세요."
                                                    """)
                            }))
    })
    @PutMapping("/payment")
    ResponseEntity<String> payment(@Validated @RequestBody PaymentPendingRequestDto requestDto);

}
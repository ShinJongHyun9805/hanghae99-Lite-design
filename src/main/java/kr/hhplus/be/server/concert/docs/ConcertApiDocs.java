package kr.hhplus.be.server.concert.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import kr.hhplus.be.server.concert.dto.ConcertListResponseDto;
import kr.hhplus.be.server.concert.dto.ConcertReserveRequestDto;
import kr.hhplus.be.server.concert.dto.ConcertScheduleListResponseDto;
import kr.hhplus.be.server.member.dto.MemberJoinRequestDto;
import kr.hhplus.be.server.member.dto.MemberJoinResponseDto;
import kr.hhplus.be.server.point.dto.PointChargeRequestDto;
import kr.hhplus.be.server.point.dto.PointChargeResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface ConcertApiDocs {

    @Operation(summary = "콘서트 목록 조회", description = "콘서트 목록을 조회한다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "콘서트 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConcertListResponseDto.class),
                            examples = @ExampleObject(value = """
                                    [
                                        {
                                            "id" : 1,
                                            "title" : "wanna see you dance again"
                                        },
                                        {
                                            "id" : 2,
                                            "title" : "머무름 - 서울"
                                        }
                                    ]
                                    
                                    """))),
            @ApiResponse(responseCode = "500", description = "진행 중인 콘서트가 없는 경우",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            value = """
                                                    "현재 진행 중인 콘서트가 없습니다."
                                                    """)
                            }))
    })
    @GetMapping("/list")
    ResponseEntity<List<ConcertListResponseDto>> getList();

    @Operation(summary = "해당 콘서트 예약 가능한 날짜 및 좌석 조회", description = "해당 콘서트의 예약 가능한 날짜와 해당 날짜의 예약 가능한 좌석정보를 조회한다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "콘서트 목록 조회 성공",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ConcertScheduleListResponseDto.class),
                            examples = @ExampleObject(
                                    name = "콘서트 예약 좌석 예시",
                                    summary = "좌석 상태 예시 데이터",
                                    value = """
                                                [
                                                    {
                                                        "concertScheduleId": 1,
                                                        "concertDate": "2025-11-02",
                                                        "seats": [
                                                            {
                                                                "seatId": 1,
                                                                "seatStatus": "AVAILABLE",
                                                                "lockedUserId": "",
                                                                "lockedAt": ""
                                                            },
                                                            {
                                                                "seatId": 2,
                                                                "seatStatus": "LOCKED",
                                                                "lockedUserId": "park",
                                                                "lockedAt": "2025-11-02 23:32:30"
                                                            },
                                                            {
                                                                "seatId": 3,
                                                                "seatStatus": "RESERVED",
                                                                "lockedUserId": "shin",
                                                                "lockedAt": "2025-11-02 23:32:30"
                                                            }
                                                        ]
                                                    }
                                                ]
                                                """,
                                    description = "좌석 상태에 따라 AVAILABLE, LOCKED, RESERVED 로 구분됩니다."
                            )))
    })
    @GetMapping("/{concertId}")
    ResponseEntity<ConcertScheduleListResponseDto> getConcertInfo(@PathVariable("concertId") Long concertId, @RequestParam(name = "concertDate", required = false) String concertDate);

    @Operation(summary = "좌석 예약 요청", description = "예약 가능한 좌석에 대해 예약 요청 한다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "예약 성공",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                    "예약 완료"
                                    """))),
            @ApiResponse(responseCode = "400", description = "예약 실패",
                    content = @Content(mediaType = "application/json",
                            examples = {
                                    @ExampleObject(name = "콘서트 ID 누락",
                                            value = """
                                                    "concertId는 필수 입력 값입니다."
                                                    """),
                                    @ExampleObject(name = "스케쥴 ID 누락",
                                            value = """
                                                    "scheduleId는 필수 입력 값입니다."
                                                    """),
                                    @ExampleObject(name = "좌석 ID 누락",
                                            value = """
                                                    "seat ID는 필수 입력 값입니다."
                                                    """),
                                    @ExampleObject(name = "userId 누락",
                                            value = """
                                                    "userId는 필수 입력 값입니다."
                                                    """)
                            }))
    })
    @PostMapping("/request-reserve")
    ResponseEntity<String> requestReserveConcert(@Validated @RequestBody ConcertReserveRequestDto requestDto);

}
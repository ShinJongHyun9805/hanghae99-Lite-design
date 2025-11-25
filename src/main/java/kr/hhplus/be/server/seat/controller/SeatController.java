package kr.hhplus.be.server.seat.controller;

import kr.hhplus.be.server.seat.dto.SeatDto;
import kr.hhplus.be.server.seat.dto.SeatDto.seatReservationRequestDto;
import kr.hhplus.be.server.seat.dto.SeatDto.seatResponseDto;
import kr.hhplus.be.server.seat.servcie.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seat")
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{concertScheduleId}")
    public ResponseEntity<seatResponseDto> getAvailableSeat(@PathVariable("concertScheduleId") Long concertScheduleId) {
        return ResponseEntity.ok(seatService.getSeat(concertScheduleId));
    }

    @PostMapping("/request")
    public ResponseEntity<String> seatReservationRequest(@RequestBody seatReservationRequestDto requestDto, @AuthenticationPrincipal UserDetails userDetails) {
        seatService.seatReservationRequest(requestDto, userDetails);

        return ResponseEntity.ok("좌석 예약에 성공했습니다.");
    }

}

package kr.hhplus.be.server.seat.controller;

import kr.hhplus.be.server.seat.dto.SeatDto.seatResponseDto;
import kr.hhplus.be.server.seat.servcie.SeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seat")
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{concertScheduleId}")
    public ResponseEntity<seatResponseDto> getAvailableSeat(@PathVariable("concertScheduleId") Long concertScheduleId) {
        return ResponseEntity.ok(seatService.getSeat(concertScheduleId));
    }

}

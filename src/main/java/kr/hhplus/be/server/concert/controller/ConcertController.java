package kr.hhplus.be.server.concert.controller;


import kr.hhplus.be.server.concert.dto.ConcertDto.ConcertListResponse;
import kr.hhplus.be.server.concert.dto.ConcertReserveRequestDto;
import kr.hhplus.be.server.concert.dto.ConcertScheduleListResponseDto;
import kr.hhplus.be.server.concert.service.ConcertService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/concert")
public class ConcertController  {

    private final ConcertService concertService;

    @GetMapping("/list")
    public ResponseEntity<List<ConcertListResponse>> getConcertList() {
        return ResponseEntity.ok(concertService.getConcertList());
    }

    @GetMapping("/{concertId}")
    public ResponseEntity<ConcertScheduleListResponseDto> getConcertInfo(@PathVariable("concertId") Long concertId,  @RequestParam(name = "concertDate", required = false) String concertDate) {
        return null;
    }

    @PostMapping("/request-reserve")
    public ResponseEntity<String> requestReserveConcert(@Validated @RequestBody ConcertReserveRequestDto requestDto) {

        return null;
    }


}

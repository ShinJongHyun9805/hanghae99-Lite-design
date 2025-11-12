package kr.hhplus.be.server.concert.controller;


import kr.hhplus.be.server.concert.docs.ConcertApiDocs;
import kr.hhplus.be.server.concert.dto.ConcertListResponseDto;
import kr.hhplus.be.server.concert.dto.ConcertReserveRequestDto;
import kr.hhplus.be.server.concert.dto.ConcertScheduleListResponseDto;
import kr.hhplus.be.server.point.dto.PointChargeRequestDto;
import kr.hhplus.be.server.point.dto.PointChargeResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/concert")
public class ConcertController implements ConcertApiDocs {

    @Override
    @GetMapping("/list")
    public ResponseEntity<List<ConcertListResponseDto>> getList() {
        return null;
    }

    @Override
    @GetMapping("/{concertId}")
    public ResponseEntity<ConcertScheduleListResponseDto> getConcertInfo(@PathVariable("concertId") Long concertId,  @RequestParam(name = "concertDate", required = false) String concertDate) {
        return null;
    }

    @Override
    @PostMapping("/request-reserve")
    public ResponseEntity<String> requestReserveConcert(@Validated @RequestBody ConcertReserveRequestDto requestDto) {

        return null;
    }


}

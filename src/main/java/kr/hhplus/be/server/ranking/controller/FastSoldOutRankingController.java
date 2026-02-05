package kr.hhplus.be.server.ranking.controller;

import kr.hhplus.be.server.ranking.dto.FastSoldOutRankingDto.FastSoldOutRankingResponse;
import kr.hhplus.be.server.ranking.service.FastSoldOutRankingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/rankings")
public class FastSoldOutRankingController {

    private final FastSoldOutRankingService fastSoldOutRankingService;

    @GetMapping("/fast-soldout")
    public ResponseEntity<List<FastSoldOutRankingResponse>> getFastSoldOutRanking(
            @RequestParam("date") String date,
            @RequestParam(name = "limit", defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(fastSoldOutRankingService.getDailyRanking(date, limit));
    }
}

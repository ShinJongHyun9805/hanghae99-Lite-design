package kr.hhplus.be.server.point.controller;


import jakarta.validation.Valid;
import kr.hhplus.be.server.point.docs.PointApiDocs;
import kr.hhplus.be.server.point.dto.PointChargeRequestDto;
import kr.hhplus.be.server.point.dto.PointChargeResponseDto;
import kr.hhplus.be.server.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/point")
public class PointController implements PointApiDocs {

    private final PointService pointService;

    @Override
    @PostMapping("/charge")
    public ResponseEntity<PointChargeResponseDto> chargePoint(@Valid @RequestBody PointChargeRequestDto requestDto) {
        return ResponseEntity.ok(pointService.chargePoint(requestDto));
    }


    @Override
    @GetMapping("/{memberId}")
    public ResponseEntity<PointChargeResponseDto> getPoint(@PathVariable("memberId") String memberId) {
        return ResponseEntity.ok(pointService.getPoint(memberId));
    }

}

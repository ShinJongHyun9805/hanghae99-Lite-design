package kr.hhplus.be.server.point.controller;


import jakarta.validation.Valid;
import kr.hhplus.be.server.point.docs.PointApiDocs;
import kr.hhplus.be.server.point.dto.PointChargeRequestDto;
import kr.hhplus.be.server.point.dto.PointChargeResponseDto;
import kr.hhplus.be.server.point.dto.PointDto;
import kr.hhplus.be.server.point.dto.PointDto.PointChargeRequest;
import kr.hhplus.be.server.point.dto.PointDto.pointResponse;
import kr.hhplus.be.server.point.service.PointService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/point")
public class PointController {

    private final PointService pointService;

    @PostMapping("/charge")
    public ResponseEntity<pointResponse> chargePoint(@Valid @RequestBody PointChargeRequest requestDto, @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(pointService.chargePoint(requestDto, userDetails));
    }

    @GetMapping("/{memberId}")
    public ResponseEntity<pointResponse> getPoint(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(pointService.getPoint(userDetails));
    }

}

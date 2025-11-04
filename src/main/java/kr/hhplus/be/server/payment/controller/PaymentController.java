package kr.hhplus.be.server.payment.controller;


import kr.hhplus.be.server.concert.docs.ConcertApiDocs;
import kr.hhplus.be.server.concert.dto.ConcertListResponseDto;
import kr.hhplus.be.server.concert.dto.ConcertReserveRequestDto;
import kr.hhplus.be.server.concert.dto.ConcertScheduleListResponseDto;
import kr.hhplus.be.server.payment.docs.PaymentApiDocs;
import kr.hhplus.be.server.payment.dto.PaymentPendingRequestDto;
import kr.hhplus.be.server.payment.dto.PaymentPendingResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment")
public class PaymentController implements PaymentApiDocs {


    @Override
    @GetMapping("/history/{memberId}")
    public ResponseEntity<List<PaymentPendingResponseDto>> getPaymentHistory(@PathVariable("memberId") String memberId){
        return null;
    }

    @Override
    @PutMapping("/payment")
    public ResponseEntity<String> payment(@Validated @RequestBody PaymentPendingRequestDto requestDto) {
        return null;
    }
}

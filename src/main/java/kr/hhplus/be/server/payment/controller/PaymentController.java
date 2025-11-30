package kr.hhplus.be.server.payment.controller;


import kr.hhplus.be.server.payment.dto.PaymentDto.PaymentAllListResponse;
import kr.hhplus.be.server.payment.dto.PaymentDto.PaymentCompleteResponse;
import kr.hhplus.be.server.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payment")
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/history/{memberId}")
    public ResponseEntity<PaymentAllListResponse> getPaymentHistory(@PathVariable("memberId") Long memberId){
        return ResponseEntity.ok(paymentService.getPaymentHistory(memberId));
    }

    @PutMapping("/payment/{paymentId}")
    public ResponseEntity<PaymentCompleteResponse> payment(@PathVariable("paymentId") Long paymentId) {
        return ResponseEntity.ok(paymentService.completePayment(paymentId));
    }
}

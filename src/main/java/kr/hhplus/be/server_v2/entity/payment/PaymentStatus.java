package kr.hhplus.be.server_v2.entity.payment;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStatus {

    PENDING("결제 대기"),
    PAYMENT("결제 완료"),
    CANCEL("결제 취소");

    private final String displayName;
}

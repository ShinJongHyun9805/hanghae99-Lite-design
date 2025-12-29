package kr.hhplus.be.server_v2.adapters.gateway;

import kr.hhplus.be.server_v2.entity.payment.Payment;

public interface PaymentRepositoryPort {

    Payment save(Payment payment);
}

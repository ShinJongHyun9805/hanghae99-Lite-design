package kr.hhplus.be.server_v2.adapters.gateway;

import kr.hhplus.be.server_v2.entity.payment.Payment;

public interface PaymentRepository {

    Payment save(Payment payment);
}

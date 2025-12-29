package kr.hhplus.be.server_v2.adapters.gateway;

import kr.hhplus.be.server_v2.entity.payment.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentJpaGateway implements PaymentRepositoryPort {

    private final PaymentRepository repository;

    @Override
    public Payment save(Payment payment) {
        return repository.save(payment);
    }
}

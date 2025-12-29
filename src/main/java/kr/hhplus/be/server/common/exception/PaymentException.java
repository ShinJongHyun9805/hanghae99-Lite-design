package kr.hhplus.be.server.common.exception;

public class PaymentException extends RuntimeException {

    public PaymentException() {

    }

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException InvalidPaymentException() {
        return new PaymentException("해당 결제 내역이 존재하지 않습니다.");
    }
}

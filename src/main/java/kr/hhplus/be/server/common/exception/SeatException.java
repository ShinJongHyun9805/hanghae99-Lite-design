package kr.hhplus.be.server.common.exception;

public class SeatException extends RuntimeException {

    public SeatException(String message) {
        super(message);
    }

    public SeatException() {

    }

    public SeatException NotExistsSeatException() {
        return new SeatException("유효하지 않은 좌석입니다.");
    }

    public SeatException ReservedSeatException() {
        return new SeatException("이미 예약된 좌석입니다.");
    }

    public SeatException InvalidSeatException() {
        return new SeatException("해당 결제 내역이 존재하지 않습니다.");
    }
}

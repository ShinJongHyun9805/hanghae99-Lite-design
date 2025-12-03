package kr.hhplus.be.server.common.exception;

public class InvalidUserException extends RuntimeException {

    public InvalidUserException() {
        super("유효하지 않은 회원입니다.");
    }

    public InvalidUserException(String message) {
        super(message);
    }
}

package kr.hhplus.be.server.common.exception;

/**
 * 분산락 획득 실패 예외
 */
public class DistributedLockException extends RuntimeException {
    public DistributedLockException(String message) {
        super(message);
    }
}

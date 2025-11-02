package kr.hhplus.be.server.common.advice;

import kr.hhplus.be.server.common.exception.DuplicateMemberException;
import kr.hhplus.be.server.common.exception.InvalidUserException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(value = InvalidUserException.class)
    public ResponseEntity<String> InvalidUserException(InvalidUserException e) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(e.getMessage());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<String> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(ex -> ex.getField() + " " + ex.getDefaultMessage())
                .findFirst()
                .orElse("필수값이 누락되었습니다.");

        return ResponseEntity.badRequest().body(message);
    }

    @ExceptionHandler(value = DuplicateMemberException.class)
    public ResponseEntity<String> DuplicateMemberException(DuplicateMemberException e) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(e.getMessage());
    }
}

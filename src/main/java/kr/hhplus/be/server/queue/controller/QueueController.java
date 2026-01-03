package kr.hhplus.be.server.queue.controller;

import kr.hhplus.be.server.queue.service.QueueTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/queue")
public class QueueController {

    private final QueueTokenService queueTokenService;

    @PostMapping("/token")
    public ResponseEntity<String> issueQueueToken(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        String token = queueTokenService.issueToken(userDetails.getUsername());
        return ResponseEntity.ok(token);
    }
}

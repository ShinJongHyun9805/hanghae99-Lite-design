package kr.hhplus.be.server.queue.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class QueueToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String token;          // UUID

    @Column(nullable = false)
    private String memberId;

    @Enumerated(EnumType.STRING)
    private QueueStatus status;

    private LocalDateTime issuedAt;
    private LocalDateTime expiredAt;
}

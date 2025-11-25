package kr.hhplus.be.server.payment.domain;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter @Setter
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private Long concertId;

    private Long concertScheduleId;

    private Long seatId;

    private Long memberId;

    private int price;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime paymentAt;

    private String cancelReason;
}

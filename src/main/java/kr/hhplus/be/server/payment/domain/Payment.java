package kr.hhplus.be.server.payment.domain;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    private Long concertId;

    private Long concertScheduleId;

    private Long seatId;

    private String memberId;

    private int price;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus;

    private LocalDateTime paymentAt;

    private String cancelReason;
}

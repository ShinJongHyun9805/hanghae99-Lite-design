package kr.hhplus.be.server_v2.entity.seat;

import jakarta.persistence.*;
import kr.hhplus.be.server_v2.entity.concert_schedule.ConcertSchedule;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long seatId;

    private int seatNumber;

    @Enumerated(EnumType.STRING)
    private SeatStatus seatStatus;

    private Long lockedUserId;

    private LocalDateTime lockedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_schedule_id")
    private ConcertSchedule schedule;
}
package kr.hhplus.be.server_v2.entity.concert_schedule;

import jakarta.persistence.*;
import kr.hhplus.be.server_v2.entity.concert.Concert;
import kr.hhplus.be.server_v2.entity.seat.Seat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
public class ConcertSchedule {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long concertScheduleId;

    LocalDateTime concertDate;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id")
    private Concert concert;

    @OneToMany(mappedBy = "schedule")
    private List<Seat> seats = new ArrayList<>();

    public void addSeat(Seat seat) {
        seats.add(seat);            // 부모 → 자식 방향
        seat.setSchedule(this);     // 자식 → 부모 방향 (FK 세팅)
    }
}

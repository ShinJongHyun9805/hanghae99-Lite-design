package kr.hhplus.be.server.concert.domain;

import jakarta.persistence.*;
import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    String title;                   // 콘서트 명

    String venueName;               // 콘서트 장소 명
    LocalDateTime created_dt;       // 등록 일자
    int price;                      // 금액
    
    String openYn;                  // 콘서트 진행 여부

    @OneToMany(mappedBy = "concert")
    List<ConcertSchedule> schedules = new ArrayList<>();

    public void addSchedule(ConcertSchedule schedule) {
        schedules.add(schedule);     // 부모 → 자식 방향
        schedule.setConcert(this);   // 자식 → 부모 방향 (FK 세팅)
    }
}

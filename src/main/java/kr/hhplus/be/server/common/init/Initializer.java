package kr.hhplus.be.server.common.init;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.concert.domain.Concert;
import kr.hhplus.be.server.concert.repository.ConcertRepository;
import kr.hhplus.be.server.concert_schedule.domain.ConcertSchedule;
import kr.hhplus.be.server.concert_schedule.repository.ConcertScheduleRepository;
import kr.hhplus.be.server.seat.domain.Seat;
import kr.hhplus.be.server.seat.domain.SeatStatus;
import kr.hhplus.be.server.seat.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class Initializer implements ApplicationRunner {

    private final ConcertRepository concertRepository;
    private final ConcertScheduleRepository scheduleRepository;
    private final SeatRepository seatRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {

        if (concertRepository.count() > 0) {
            return;
        }

        initConcerts();
    }

    private void initConcerts() {

        Concert concert1 = new Concert();
        concert1.setTitle("baek yaerin WORLD TOUR CONCERT");
        concert1.setVenueName("KSPO DOME");
        concert1.setPrice(150000);
        concert1.setOpenYn("Y");
        concert1.setCreated_dt(LocalDateTime.now());

        Concert concert2 = new Concert();
        concert2.setTitle("Coldplay LIVE");
        concert2.setVenueName("Olympic Stadium");
        concert2.setPrice(180000);
        concert2.setOpenYn("Y");
        concert2.setCreated_dt(LocalDateTime.now());

        concertRepository.saveAll(List.of(concert1, concert2));

        // 3일치 스케줄 등록 (12-10, 12-11, 12-12)
        List<LocalDateTime> dates = List.of(
                LocalDateTime.of(2025, 12, 10, 19, 0),
                LocalDateTime.of(2025, 12, 11, 19, 0),
                LocalDateTime.of(2025, 12, 12, 19, 0)
        );

        createSchedules(concert1, dates);
        createSchedules(concert2, dates);

    }

    private void createSchedules(Concert concert, List<LocalDateTime> dates) {

        for (LocalDateTime date : dates) {
            ConcertSchedule concertSchedule = new ConcertSchedule();
            concertSchedule.setConcertDate(date);
            concertSchedule.setConcert(concert);

            scheduleRepository.save(concertSchedule);

            createSeats(concertSchedule);
        }

    }

    private void createSeats(ConcertSchedule concertSchedule) {
        for (int seatNo = 1; seatNo <= 50; seatNo++) {
            Seat seat = new Seat();
            seat.setSeatNumber(seatNo);
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seat.setSchedule(concertSchedule);

            seatRepository.save(seat);
        }
    }
}

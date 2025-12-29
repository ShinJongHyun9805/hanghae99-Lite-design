package kr.hhplus.be.server.concert.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

public class ConcertDto {

    public record ConcertListResponse(
            Long id,

            String title,

            String venueName,

            LocalDateTime createdAt,

            int price,

            String openYn,

            List<ConcertScheduleResult> concertScheduleList

    ) {

        @Getter @Setter
        public static class ConcertScheduleResult {

            Long concertScheduleId;

            LocalDateTime concertDate;
        }
    }

}


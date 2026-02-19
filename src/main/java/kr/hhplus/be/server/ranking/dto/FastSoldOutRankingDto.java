package kr.hhplus.be.server.ranking.dto;

import java.time.LocalDateTime;

public class FastSoldOutRankingDto {

    public record FastSoldOutRankingResponse(
            Long scheduleId,
            long durationSeconds,
            String concertTitle,
            String venueName,
            LocalDateTime concertDate
    ) {
        public FastSoldOutRankingResponse(
                Long scheduleId,
                long durationSeconds,
                String concertTitle,
                String venueName,
                LocalDateTime concertDate
        ) {
            this.scheduleId = scheduleId;
            this.durationSeconds = durationSeconds;
            this.concertTitle = concertTitle;
            this.venueName = venueName;
            this.concertDate = concertDate;
        }
    }
}

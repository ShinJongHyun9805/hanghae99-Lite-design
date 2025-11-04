package kr.hhplus.be.server.concert.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "콘서트 조회 응답 DTO")
public class ConcertListResponseDto {

    @Schema(description = "콘서트 ID", example = "1")
    private Long id;

    @Schema(description = "콘서트명", example = "wanna see you dance again")
    private String title;
}

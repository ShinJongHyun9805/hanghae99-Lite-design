package kr.hhplus.be.server.point.dto;

public class PointDto {

    public record PointChargeRequest (

            int amt

    ){}

    public record pointResponse (
            String memberId,

            long amt
    ) {}
}

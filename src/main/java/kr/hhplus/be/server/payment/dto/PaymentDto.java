package kr.hhplus.be.server.payment.dto;

import java.time.LocalDateTime;
import java.util.List;

public class PaymentDto {

    public record PaymentAllListResponse(
        List<PaymentListResult> pendingList,

        List<PaymentListResult> completeList,

        List<PaymentListResult> cancelList
    ) {
        public PaymentAllListResponse(List<PaymentListResult> pendingList, List<PaymentListResult> completeList, List<PaymentListResult> cancelList){
            this.pendingList = pendingList;
            this.completeList = completeList;
            this.cancelList = cancelList;
        }

    }


    public record PaymentListResult(
            Long paymentId,

            String concertTitle,

            String venueName,

            int price,

            String paymentStatus,

            LocalDateTime modDt
    ) {
        public PaymentListResult(Long paymentId, String concertTitle, String venueName, int price, String paymentStatus, LocalDateTime modDt) {
            this.paymentId = paymentId;
            this.concertTitle = concertTitle;
            this.venueName = venueName;
            this.price = price;
            this.paymentStatus = paymentStatus;
            this.modDt = modDt;
        }
    }

    public record PaymentCompleteResponse(

            Long paymentId,

            String concertTitle,

            String venueName,

            int price,

            String paymentStatus,

            LocalDateTime paymentAt

    ){
        public PaymentCompleteResponse(Long paymentId, String concertTitle, String venueName, int price, String paymentStatus, LocalDateTime paymentAt) {
            this.paymentId = paymentId;
            this.concertTitle = concertTitle;
            this.venueName = venueName;
            this.price = price;
            this.paymentStatus = paymentStatus;
            this.paymentAt = paymentAt;
        }
    }
}

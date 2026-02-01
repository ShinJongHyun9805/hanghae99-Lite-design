package kr.hhplus.be.server.point.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Point {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String memberId;

    private int pointAmt;

    public void chargePoint(int amount) {
        this.pointAmt += amount;
    }

    public void usePoint(int amount) {
        if (this.pointAmt < amount) {
            throw new IllegalArgumentException("포인트가 부족합니다.");
        }
        this.pointAmt -= amount;
    }
}

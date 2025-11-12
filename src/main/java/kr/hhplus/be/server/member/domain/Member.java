package kr.hhplus.be.server.member.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String memberId;

    private String memberName;

    public Member joinMember(String memberId, String memberName) {
        Member member = new Member();
        member.setMemberId(memberId);
        member.setMemberName(memberName);

        return member;
    }

}

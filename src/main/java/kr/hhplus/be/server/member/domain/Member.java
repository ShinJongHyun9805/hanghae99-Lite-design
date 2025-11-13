package kr.hhplus.be.server.member.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Member {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String memberId;

    private String password;

    private String memberName;

    /**
     * 유저가 여러 역할(ROLE_USER, ROLE_ADMIN 등)을 가질 수 있도록
     * 별도 컬렉션 테이블(user_roles)로 관리
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "member_roles",                // 컬렉션 저장 테이블명
            joinColumns = @JoinColumn(name = "member_id") // FK 컬럼명
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Set<Role> roles = new HashSet<>();

}

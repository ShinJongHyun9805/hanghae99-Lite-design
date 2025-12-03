package kr.hhplus.be.server_v2.entity.member;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum  Role {

    MEMBER("일반회원"),
    ADMIN("관리자");

    private final String displayName;
}

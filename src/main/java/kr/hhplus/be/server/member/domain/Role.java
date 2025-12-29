package kr.hhplus.be.server.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum  Role {

    MEMBER("일반회원"),
    ADMIN("관리자");

    private final String displayName;
}

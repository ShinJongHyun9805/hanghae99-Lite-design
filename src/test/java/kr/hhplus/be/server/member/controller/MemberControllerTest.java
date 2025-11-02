package kr.hhplus.be.server.member.controller;

import jakarta.transaction.Transactional;
import kr.hhplus.be.server.common.exception.DuplicateMemberException;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.dto.MemberJoinRequestDto;
import kr.hhplus.be.server.member.dto.MemberJoinResponseDto;
import kr.hhplus.be.server.member.service.MemberService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class MemberControllerTest {

    @Autowired
    private MemberService memberService;

    @Test
    @Transactional
    void 중복_회원_시_500_이미_가입된_회원_메세지() {
        MemberJoinRequestDto requestDto = new MemberJoinRequestDto();
        requestDto.setMemberId("shin");
        requestDto.setName("jonghyun");

        ResponseEntity<MemberJoinResponseDto> result = memberService.join(requestDto);
        assertEquals(result.getBody().getMemberId(), requestDto.getMemberId());

        DuplicateMemberException dme = assertThrows(DuplicateMemberException.class, () -> {
            memberService.join(requestDto);
        });

        Assertions.assertTrue(dme instanceof DuplicateMemberException);
    }

}
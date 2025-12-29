package kr.hhplus.be.server.point.service;

import kr.hhplus.be.server.common.exception.InvalidUserException;
import kr.hhplus.be.server.member.domain.Member;
import kr.hhplus.be.server.member.repository.MemberRepository;
import kr.hhplus.be.server.point.domain.Point;
import kr.hhplus.be.server.point.dto.PointChargeRequestDto;
import kr.hhplus.be.server.point.dto.PointChargeResponseDto;
import kr.hhplus.be.server.point.dto.PointDto;
import kr.hhplus.be.server.point.dto.PointDto.PointChargeRequest;
import kr.hhplus.be.server.point.dto.PointDto.pointResponse;
import kr.hhplus.be.server.point.repository.PointRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

class PointServiceTest {

    @Mock
    private PointRepository pointRepository;

    @Mock
    private MemberRepository userRepository;

    @InjectMocks
    private PointService pointService;

    UserDetails userDetails = new UserDetails() {
        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
        }

        @Override
        public String getPassword() {
            return null;
        }

        @Override
        public String getUsername() {
            return "shin";
        }
    };

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void 존재하지_않는_회원이면_InvalidUserException_발생() {

        PointChargeRequest dto = new PointChargeRequest(1000);

        given(userRepository.findByMemberId("shin")).willReturn(Optional.empty());

        assertThrows(InvalidUserException.class, () -> pointService.chargePoint(dto, userDetails));

        verify(userRepository, times(1)).findForUpdateByMemberId("shin");
        verify(pointRepository, never()).save(any());
    }

    @Test
    void 기존_포인트가_있으면_금액을_추가하고_저장() {

        Member member = new Member();
        member.setMemberId("shin");

        Point point = new Point();
        point.setMemberId("shin");
        point.setPointAmt(1000);

        PointChargeRequest dto = new PointChargeRequest(1000);

        given(userRepository.findForUpdateByMemberId("shin")).willReturn(Optional.of(member));
        given(pointRepository.findForUpdateByMemberId("shin")).willReturn(Optional.of(point));
        given(pointRepository.save(any(Point.class))).willAnswer(invocation -> invocation.getArgument(0));

        pointResponse pointChargeResponseDto = pointService.chargePoint(dto, userDetails);

        assertEquals(2000, pointChargeResponseDto.amt());
    }

    @Test
    void 포인트_정보가_없으면_새_Point를_생성하여_저장() {

        Member member = new Member();
        member.setMemberId("shin");
        given(userRepository.findForUpdateByMemberId("shin")).willReturn(Optional.of(member));

        Point np = new Point();
        np.setMemberId(member.getMemberId());
        np.setPointAmt(0);
        given(pointRepository.findForUpdateByMemberId("shin")).willReturn(Optional.of(np));

        PointChargeRequest dto = new PointChargeRequest(1000);
        pointResponse pointChargeResponseDto = pointService.chargePoint(dto, userDetails);

        assertEquals("shin", pointChargeResponseDto.memberId());
        assertEquals(1000, pointChargeResponseDto.amt());
    }
}
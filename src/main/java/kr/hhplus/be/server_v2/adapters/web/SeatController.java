package kr.hhplus.be.server_v2.adapters.web;

import kr.hhplus.be.server.seat.dto.SeatDto;
import kr.hhplus.be.server_v2.usecase.lockseat.LockSeatInputPort;
import kr.hhplus.be.server_v2.usecase.lockseat.LockSeatInputPort.LockSeatCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/seat")
public class SeatController {

    private final LockSeatInputPort useCase;
    private final LockSeatPresenter presenter;

    @PostMapping("/request")
    public ResponseEntity<?> reserve(
            @RequestBody SeatDto.seatReservationRequestDto req,
            @AuthenticationPrincipal UserDetails user
    ) {
        useCase.lockSeat(new LockSeatCommand(
                req.concertScheduleId(),
                req.seatId(),
                user.getUsername()
        ));

        if (presenter.getError() != null) {
            return ResponseEntity.badRequest().body(presenter.getError());
        }

        return ResponseEntity.ok(presenter.getResult());
    }
}

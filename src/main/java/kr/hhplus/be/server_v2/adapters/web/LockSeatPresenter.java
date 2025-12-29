package kr.hhplus.be.server_v2.adapters.web;

import kr.hhplus.be.server_v2.usecase.lockseat.LockSeatOutputPort;
import lombok.Getter;

public class LockSeatPresenter implements LockSeatOutputPort {

    @Getter
    private LockSeatResult result;

    @Getter
    private String error;

    @Override
    public void success(LockSeatResult result) {
        this.result = result;
        this.error = null;
    }

    @Override
    public void fail(String message) {
        this.result = null;
        this.error = message;
    }
}

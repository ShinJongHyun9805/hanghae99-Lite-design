package kr.hhplus.be.server.point.service;

import kr.hhplus.be.server.point.dto.PointDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static kr.hhplus.be.server.point.dto.PointDto.pointResponse;
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ConcurrencyPointServiceTest {

    @Autowired
    PointService pointService;

    UserDetails userDetails = new UserDetails() {
        @Override public String getUsername() { return "park"; }
        @Override public String getPassword() { return ""; }
        @Override public Collection<? extends GrantedAuthority> getAuthorities() { return null; }
        @Override public boolean isAccountNonExpired() { return true; }
        @Override public boolean isAccountNonLocked() { return true; }
        @Override public boolean isCredentialsNonExpired() { return true; }
        @Override public boolean isEnabled() { return true; }
    };

    @Test
    void 포인트_충전_동시성_테스트() throws Exception {

        int threadCount = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(20);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    pointService.chargePoint(new PointDto.PointChargeRequest(1), userDetails);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        pointResponse res = pointService.getPoint(userDetails);

        Assertions.assertEquals(100, res.amt());
    }
}

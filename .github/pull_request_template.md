# 시나리오 선택
- 콘서트 예약 서비스

## [STEP-1] 회원 가입 구현
### PR 설명
- sign-up
  - 회원가입, 로그인 기능 추가
    - Spring security + jwt 사용

### Definition of Done (DoD)
  - WebMvcTest
    - 회원 가입 -> 로그인 -> 내정보 조회 테스트 확인
    - 회원 가입, 로그인 실패 케이스 검증 추가
------------------------------------------

## [STEP-2] 포인트 충전 / 조회 구현
### PR 설명
- point
  - 포인트 충전, 조회 기능 추가
    - 충전 시 동시성 해결을 위한  @Lock(LockModeType.PESSIMISTIC_WRITE) 조회

### Definition of Done (DoD)
  - PointServiceTest
    - Unit Test
  - ConcurrencyPointServiceTest
    - Thread를 활용한 동시성 테스트 확인
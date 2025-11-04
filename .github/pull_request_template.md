# 시나리오 선택
- 콘서트 예약 서비스

## [STEP-1] 포인트 충전 / 조회 + 회원 가입  구현
### PR 설명
- point
  - 포인트 충전, 조회 + 회원 가입 API 명세서 작성
    - application.yml 스키마 미노출 셋팅
    - swagger 에러로 인한 spring boot version down
    
  - 포인트 충전, 조회 + 회원 가입 API 구현 

### 리뷰 포인트
- JPA를 처음 접해봤습니다. Point <-> Memember 연관관계 매핑을 하지 않기로 결정했습니다.(Q&A 읽어봄)
    - Q1) Member, Point의 Entity 설정 + 포인트 조회 시 각각의 Repository의 findByMemberId 식으로 진행했는데, 괜찮은 방법인지 궁금합니다.
### Definition of Done (DoD)
  - 회원 가입 시 중복 회원 여부 webMvc 테스트 완료
  - 포인트 충전 ㆍ 조회 유닛 테스트 완료, 포인트 충전 시 필수값 ㆍ 충전 금액 1원 이상 webMvc 테스트 완료
------------------------------------------

## [STEP-2] swagger + ERD + 인프라 구성도 + 시퀀스 다이어그램
### PR 설명
- Swagger
  - 콘서트 목록 조회 -> 해당 콘서트의 예약 가능한 날짜 및 좌석 조회 -> 좌석 예약 요청
  - 좌석 예약 요청 시 결제 대기 상태 -> 결제 내역 -> 결제 요청 명세 추가
  - README.md > ERD, 인프라 구성도, 시퀀스 다이어그램 추가


### 리뷰 포인트

### Definition of Done (DoD)
- swagger api 완료.
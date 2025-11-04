sequenceDiagram
autonumber
participant A as member
participant B as Point

    activate A
        A->>B: 포인트 충전 
		B-->>A : 충전 완료
    deactivate A

--------------------
sequenceDiagram
autonumber
participant A as member
participant B as concert
participant C as backend

    activate A
            A->>+B: 콘서트 목록 요청
            B-->>-A : 콘서트 목록 응답(예약 가능한 날짜 & 예약 가능한 좌석)  
		A->>+B: 해당 좌석 예약 요청
		B->>+C: 해당 좌석 LOCKED
		C->>+C: 해당 콘서트의 결제 대기 적재
		C-->>-B: 예약 요청 완료 응답
		B-->>-A: 예약 요청 완료 응답

		A->>+B: 결제 내역 조회
		
        alt 결제 내역 없음 (case1)
            B-->>A: "결제 내역이 없습니다."
            else 결제 내역 있음 (결제 대기, case2)
            
            B-->>A: "결제 대기 중입니다."
            else 결제 내역 있음 (결제 완료, case3)
            
            B-->>A: "결제가 완료되었습니다."
            else 결제 내역 있음 (결제 취소, case4)
            
            B-->>A: "결제가 취소되었습니다."
        end
    
        A->>+B: 결제 요청
        B-->-A: 결제 완료
    
    deactivate A


-----------------
package kr.hhplus.be.server.member.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.member.dto.MemberAuthDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MemberControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @Test
    void 회원가입_로그인_내정보_조회() throws Exception {

        // 회원가입
        mvc.perform(post("/api/v1/member/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(new MemberAuthDto.SignUpRequest("shin", "1234qwer", "jonghyun"))))
                .andExpect(status().isCreated());


        // 로그인
        MvcResult loginRes = mvc.perform(post("/api/v1/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new MemberAuthDto.LoginRequest("shin", "1234qwer"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andReturn();

        String token = om.readTree(loginRes.getResponse().getContentAsString()).get("accessToken").asText();
        assertThat(token).isNotBlank();

        // Me
        mvc.perform(get("/api/v1/member/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value("shin"))
                .andExpect(jsonPath("$.name").value("jonghyun"));
    }

    @Test
    void 회원가입_실패() throws Exception {
        mvc.perform(post("/api/v1/member/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new MemberAuthDto.SignUpRequest("shin", "1234qwer", "jonghyun"))))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/v1/member/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new MemberAuthDto.SignUpRequest("shin", "1234qwer", "jonghyun"))))
                .andExpect(status().isConflict());

    }

    @Test
    void 로그인_실패() throws Exception {
        mvc.perform(post("/api/v1/member/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new MemberAuthDto.LoginRequest("shin", "1234qwer"))))
                .andExpect(status().is4xxClientError());
    }
}
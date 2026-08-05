package com.victory.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.victory.security.JwtUtil;
import com.victory.service.TeacherAccountManagementService;

@SpringBootTest
@AutoConfigureMockMvc
class TeacherAccountManagementSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtil jwtUtil;

    @MockitoBean
    private TeacherAccountManagementService accountManagementService;

    @Test
    void managedAccounts_rejectsStudentTokenBeforeControllerRuns() throws Exception {
        String studentToken = jwtUtil.generateToken(20L, "s01", "student");

        mockMvc.perform(get("/api/teachers/me/managed-accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        verifyNoInteractions(accountManagementService);
    }
}

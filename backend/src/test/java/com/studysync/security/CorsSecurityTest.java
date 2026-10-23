package com.studysync.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class CorsSecurityTest {

    private SecurityConfig securityConfig;
    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/api/test-cors")
    static class DummyTestController {
        @GetMapping
        public String get() {
            return "ok";
        }

        @PostMapping
        public String post() {
            return "created";
        }
    }

    @BeforeEach
    void setUp() {
        securityConfig = new SecurityConfig(null, null, null, null);
        CorsConfigurationSource corsSource = securityConfig.corsConfigurationSource();

        mockMvc = MockMvcBuilders
                .standaloneSetup(new DummyTestController())
                .addFilter(new CorsFilter(corsSource))
                .build();
    }

    @Test
    void corsConfigurationSource_ContainsStrictConfig() {
        CorsConfigurationSource source = securityConfig.corsConfigurationSource();
        assertNotNull(source);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/study-materials");
        CorsConfiguration config = source.getCorsConfiguration(request);

        assertNotNull(config);
        assertEquals(List.of("http://localhost:5173", "http://127.0.0.1:5173"), config.getAllowedOrigins());
        assertEquals(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"), config.getAllowedMethods());
        assertEquals(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With", "Origin"), config.getAllowedHeaders());
        assertEquals(List.of("Authorization"), config.getExposedHeaders());
        assertTrue(config.getAllowCredentials());
        assertEquals(3600L, config.getMaxAge());
    }

    @Test
    void preflight_AllowedOrigin_ReturnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/test-cors")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Max-Age", "3600"));
    }

    @Test
    void preflight_127Origin_ReturnsCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/test-cors")
                        .header("Origin", "http://127.0.0.1:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://127.0.0.1:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void preflight_DisallowedOrigin_DoesNotReturnAllowOriginAndReturnsForbidden() throws Exception {
        mockMvc.perform(options("/api/test-cors")
                        .header("Origin", "http://malicious-site.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void actualRequest_AllowedOrigin_ReturnsCorsHeader() throws Exception {
        mockMvc.perform(get("/api/test-cors")
                        .header("Origin", "http://localhost:5173"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void actualRequest_DisallowedOrigin_DoesNotReturnAllowOriginHeader() throws Exception {
        mockMvc.perform(get("/api/test-cors")
                        .header("Origin", "http://attacker.com"))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}

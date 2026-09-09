package com.liveticket.auth.service;

import com.liveticket.auth.dto.LoginRequest;
import com.liveticket.auth.dto.RegisterRequest;
import com.liveticket.auth.service.impl.AuthServiceImpl;
import com.liveticket.auth.vo.LoginVO;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.user.entity.User;
import com.liveticket.user.mapper.UserMapper;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserMapper userMapper;
    private AuthServiceImpl authService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        authService = new AuthServiceImpl(userMapper,
                new com.liveticket.auth.JwtTokenProvider(
                        "LiveTicketDemoJwtSecretKeyMustBeLongerThan32Chars2026", 86400));
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("123456");
        request.setNickname("New User");
        return request;
    }

    @Test
    void registerSuccess() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        authService.register(registerRequest());

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User inserted = captor.getValue();
        assertThat(inserted.getUsername()).isEqualTo("newuser");
        assertThat(inserted.getPasswordHash()).isNotEqualTo("123456");
        assertThat(encoder.matches("123456", inserted.getPasswordHash())).isTrue();
        assertThat(inserted.getStatus()).isEqualTo(1);
    }

    @Test
    void registerDuplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> authService.register(registerRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.USERNAME_ALREADY_EXISTS.getCode());
    }

    private User seededUser(String rawPassword) {
        User user = new User();
        user.setId(1L);
        user.setUsername("demo01");
        user.setNickname("Alex");
        user.setStatus(1);
        user.setPasswordHash(encoder.encode(rawPassword));
        return user;
    }

    @Test
    void loginSuccessGeneratesValidJwt() {
        when(userMapper.selectOne(any())).thenReturn(seededUser("123456"));

        LoginRequest request = new LoginRequest();
        request.setUsername("demo01");
        request.setPassword("123456");

        LoginVO vo = authService.login(request);

        assertThat(vo.getToken()).isNotBlank();
        assertThat(vo.getUserId()).isEqualTo(1L);
        assertThat(vo.getUsername()).isEqualTo("demo01");

        com.liveticket.auth.JwtTokenProvider provider = new com.liveticket.auth.JwtTokenProvider(
                "LiveTicketDemoJwtSecretKeyMustBeLongerThan32Chars2026", 86400);
        Claims claims = provider.parseToken(vo.getToken());
        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("username", String.class)).isEqualTo("demo01");
    }

    @Test
    void loginWrongPassword() {
        when(userMapper.selectOne(any())).thenReturn(seededUser("123456"));

        LoginRequest request = new LoginRequest();
        request.setUsername("demo01");
        request.setPassword("wrong-password");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.PASSWORD_INCORRECT.getCode());
    }

    @Test
    void loginUnknownUser() {
        when(userMapper.selectOne(any())).thenReturn(null);

        LoginRequest request = new LoginRequest();
        request.setUsername("nobody");
        request.setPassword("123456");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.USER_NOT_FOUND.getCode());
    }

    @Test
    void expiredTokenRejected() {
        com.liveticket.auth.JwtTokenProvider expiredProvider = new com.liveticket.auth.JwtTokenProvider(
                "LiveTicketDemoJwtSecretKeyMustBeLongerThan32Chars2026", -10);

        String token = expiredProvider.createToken(1L, "demo01");

        assertThatThrownBy(() -> expiredProvider.parseToken(token))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.TOKEN_EXPIRED.getCode());
    }

    @Test
    void invalidTokenRejected() {
        com.liveticket.auth.JwtTokenProvider provider = new com.liveticket.auth.JwtTokenProvider(
                "LiveTicketDemoJwtSecretKeyMustBeLongerThan32Chars2026", 86400);

        assertThatThrownBy(() -> provider.parseToken("not-a-jwt"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ErrorCode.TOKEN_INVALID.getCode());
    }
}

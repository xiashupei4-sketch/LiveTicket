package com.liveticket.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.liveticket.auth.dto.LoginRequest;
import com.liveticket.auth.dto.RegisterRequest;
import com.liveticket.auth.service.AuthService;
import com.liveticket.auth.vo.LoginVO;
import com.liveticket.common.enums.ErrorCode;
import com.liveticket.common.exception.BusinessException;
import com.liveticket.user.entity.User;
import com.liveticket.user.mapper.UserMapper;
import com.liveticket.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void register(RegisterRequest request) {
        Long exists = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus(1);
        userMapper.insert(user);
        log.info("[AUTH] register username={} userId={}", request.getUsername(), user.getId());
    }

    @Override
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, request.getUsername()));
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.PASSWORD_INCORRECT);
        }

        String token = jwtTokenProvider.createToken(user.getId(), user.getUsername());
        log.info("[AUTH] login username={} userId={}", user.getUsername(), user.getId());
        return new LoginVO(token, user.getId(), user.getUsername(), user.getNickname());
    }
}

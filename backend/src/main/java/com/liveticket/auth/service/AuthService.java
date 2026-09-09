package com.liveticket.auth.service;

import com.liveticket.auth.dto.LoginRequest;
import com.liveticket.auth.dto.RegisterRequest;
import com.liveticket.auth.vo.LoginVO;

public interface AuthService {

    void register(RegisterRequest request);

    LoginVO login(LoginRequest request);
}

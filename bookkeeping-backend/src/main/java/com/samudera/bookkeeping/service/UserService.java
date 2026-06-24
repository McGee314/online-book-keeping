package com.samudera.bookkeeping.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.samudera.bookkeeping.dto.LoginRequest;
import com.samudera.bookkeeping.dto.RegisterRequest;
import com.samudera.bookkeeping.entity.User;
import com.samudera.bookkeeping.vo.AuthResponse;
import com.samudera.bookkeeping.vo.UserInfoVO;

public interface UserService extends IService<User> {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    UserInfoVO getCurrentUserInfo();
}
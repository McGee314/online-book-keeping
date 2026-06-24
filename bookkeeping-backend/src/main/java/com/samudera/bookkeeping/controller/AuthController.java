package com.samudera.bookkeeping.controller;

import com.samudera.bookkeeping.common.Result;
import com.samudera.bookkeeping.dto.LoginRequest;
import com.samudera.bookkeeping.dto.RegisterRequest;
import com.samudera.bookkeeping.service.UserService;
import com.samudera.bookkeeping.vo.AuthResponse;
import com.samudera.bookkeeping.vo.UserInfoVO;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(userService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(userService.login(request));
    }

    @GetMapping("/me")
    public Result<UserInfoVO> currentUser() {
        return Result.success(userService.getCurrentUserInfo());
    }
}
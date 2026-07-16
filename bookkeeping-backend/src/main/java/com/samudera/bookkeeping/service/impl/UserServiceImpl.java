package com.samudera.bookkeeping.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.samudera.bookkeeping.context.UserContext;
import com.samudera.bookkeeping.dto.LoginRequest;
import com.samudera.bookkeeping.dto.PasswordUpdateRequest;
import com.samudera.bookkeeping.dto.ProfileUpdateRequest;
import com.samudera.bookkeeping.dto.RegisterRequest;
import com.samudera.bookkeeping.entity.User;
import com.samudera.bookkeeping.exception.BusinessException;
import com.samudera.bookkeeping.mapper.UserMapper;
import com.samudera.bookkeeping.security.JwtUtil;
import com.samudera.bookkeeping.service.UserService;
import com.samudera.bookkeeping.vo.AuthResponse;
import com.samudera.bookkeeping.vo.UserInfoVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public UserServiceImpl(PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthResponse register(RegisterRequest request) {
        User existingUser = getByUsername(request.getUsername());
        if (existingUser != null) {
            throw new BusinessException(409, "Username already exists");
        }

        if (StringUtils.hasText(request.getEmail())) {
            User emailUser = lambdaQuery()
                    .eq(User::getEmail, request.getEmail())
                    .one();
            if (emailUser != null) {
                throw new BusinessException(409, "Email already exists");
            }
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setEmail(request.getEmail());
        user.setStatus(1);
        user.setDeleted(0);
        save(user);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, toUserInfo(user));
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = getByUsername(request.getUsername());
        if (user == null) {
            throw new BusinessException(401, "Invalid username or password");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "User account is disabled");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "Invalid username or password");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return new AuthResponse(token, toUserInfo(user));
    }

    @Override
    public UserInfoVO getCurrentUserInfo() {
        User user = requireCurrentUser();
        return toUserInfo(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateProfile(ProfileUpdateRequest request) {
        User user = requireCurrentUser();

        if (StringUtils.hasText(request.getNickname())) {
            user.setNickname(request.getNickname().trim());
        }

        if (StringUtils.hasText(request.getEmail())) {
            // Check email uniqueness (excluding current user)
            User emailUser = lambdaQuery()
                    .eq(User::getEmail, request.getEmail().trim())
                    .ne(User::getId, user.getId())
                    .one();
            if (emailUser != null) {
                throw new BusinessException(409, "Email already in use by another account");
            }
            user.setEmail(request.getEmail().trim());
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar().trim());
        }

        updateById(user);
        return toUserInfo(getById(user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(String avatarUrl) {
        User user = requireCurrentUser();
        user.setAvatar(avatarUrl);
        updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(PasswordUpdateRequest request) {
        User user = requireCurrentUser();

        // Verify old password
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "Current password is incorrect");
        }

        // Confirm new passwords match
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException(400, "New password and confirm password do not match");
        }

        // Hash and save the new password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        updateById(user);
    }

    private User requireCurrentUser() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "Unauthorized");
        }
        User user = getById(userId);
        if (user == null) {
            throw new BusinessException(404, "User not found");
        }
        return user;
    }

    private User getByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
    }

    private UserInfoVO toUserInfo(User user) {
        return new UserInfoVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatar()
        );
    }
}
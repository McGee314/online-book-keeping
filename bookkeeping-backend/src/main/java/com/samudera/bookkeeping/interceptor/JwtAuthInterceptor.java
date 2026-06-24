package com.samudera.bookkeeping.interceptor;

import com.samudera.bookkeeping.context.UserContext;
import com.samudera.bookkeeping.exception.BusinessException;
import com.samudera.bookkeeping.security.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public JwtAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new BusinessException(401, "Missing or invalid Authorization header");
        }

        String token = authorization.substring(7);
        try {
            Long userId = jwtUtil.getUserId(token);
            UserContext.setUserId(userId);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(401, "Invalid or expired token");
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}
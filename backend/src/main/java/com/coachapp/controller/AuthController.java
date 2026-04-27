package com.coachapp.controller;

import com.coachapp.dto.ApiResponse;
import com.coachapp.dto.auth.AuthResponse;
import com.coachapp.dto.auth.CoachRegisterRequest;
import com.coachapp.dto.auth.LoginRequest;
import com.coachapp.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register/coach")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> registerCoach(@Valid @RequestBody CoachRegisterRequest request,
                                                   HttpServletResponse response) {
        return ApiResponse.success(authService.registerCoach(request, response));
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                           HttpServletResponse response) {
        return ApiResponse.success(authService.login(request, response));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            throw new BadCredentialsException("Missing refresh token");
        }
        return ApiResponse.success(authService.refresh(refreshToken, response));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @CookieValue(name = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken, response);
        return ApiResponse.success(null);
    }
}

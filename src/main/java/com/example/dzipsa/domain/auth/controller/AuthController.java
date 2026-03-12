package com.example.dzipsa.domain.auth.controller;

import com.example.dzipsa.domain.auth.dto.request.RefreshTokenRequest;
import com.example.dzipsa.domain.auth.dto.response.AccessTokenResponse;
import com.example.dzipsa.domain.auth.dto.response.TokenResponse;
import com.example.dzipsa.domain.auth.service.AuthService;
import com.example.dzipsa.domain.user.dto.response.UserResponse;
import com.example.dzipsa.domain.user.entity.User;
import com.example.dzipsa.domain.user.service.UserService;
import com.example.dzipsa.global.exception.dto.ErrorResponse;
import com.example.dzipsa.global.util.CookieUtil;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Auth", description = "인증 관련 API (로그인, 로그아웃, 토큰 재발급 등)")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final CookieUtil cookieUtil;

    @GetMapping("/check")
    @Operation(summary = "Access Token 유효성 검증", description = "헤더의 Access Token이 유효한지 검증합니다. 유효하면 200 OK, 아니면 401 Unauthorized를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "토큰 유효함"),
            @ApiResponse(responseCode = "401", description = "토큰 만료 또는 유효하지 않음", 
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> check(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        log.info("[AuthController] check 요청 user={}", user != null ? user.getId() : "unknown");
        return ResponseEntity.ok().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "쿠키의 Refresh Token을 사용하여 새로운 Access Token을 발급합니다. (Rotation: Refresh Token도 갱신됨)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "재발급 성공 (Body: Access Token, Cookie: Refresh Token)"),
            @ApiResponse(responseCode = "401", description = "Refresh Token이 없거나 유효하지 않음", 
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<AccessTokenResponse> refresh(
            @Parameter(description = "Refresh Token (Cookie)", required = false) 
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("[AuthController] refresh 요청");

        if (refreshToken == null) {
            log.warn("[AuthController] refresh 실패 - 쿠키에 refreshToken 없음");
            return ResponseEntity.status(401).build();
        }

        String oldAccessToken = resolveToken(request);
        TokenResponse tokenResponse = authService.refresh(new RefreshTokenRequest(refreshToken), oldAccessToken);

        // Refresh Token 쿠키 생성
        ResponseCookie refreshTokenCookie = cookieUtil.createRefreshTokenCookie(tokenResponse.getRefreshToken());
        response.addHeader("Set-Cookie", refreshTokenCookie.toString());

        // Access Token은 응답 바디로 반환
        AccessTokenResponse accessTokenResponse = AccessTokenResponse.builder()
                .accessToken(tokenResponse.getAccessToken())
                .build();

        return ResponseEntity.ok(accessTokenResponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token 삭제 및 Access Token 블랙리스트 처리")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    })
    public ResponseEntity<Void> logout(
            @Parameter(description = "Refresh Token (Cookie)", required = false)
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletRequest request,
            HttpServletResponse response) {
        log.info("[AuthController] logout 요청");
        String accessToken = resolveToken(request);
        
        authService.logout(refreshToken, accessToken);
        
        // 쿠키 삭제
        response.addHeader("Set-Cookie", cookieUtil.deleteRefreshTokenCookie().toString());

        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    @Operation(summary = "내 정보 조회", description = "현재 로그인한 사용자의 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자", 
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<UserResponse> me(
            @Parameter(hidden = true) @AuthenticationPrincipal User user) {
        log.info("[AuthController] me 요청 user={}", user != null ? user.getId() : "null");
        if (user == null) {
            log.warn("[AuthController] me 인증 없음");
            return ResponseEntity.status(401).build();
        }
        UserResponse response = userService.findById(user.getId());
        return ResponseEntity.ok(response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}

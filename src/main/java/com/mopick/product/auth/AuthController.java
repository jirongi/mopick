package com.mopick.product.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/naver/authorize-url")
    public AuthorizeUrlResponse naverAuthorizeUrl() {
        return new AuthorizeUrlResponse(authService.buildNaverAuthorizeUrl());
    }

    @GetMapping("/naver/callback")
    public AuthService.AuthResponse naverCallback(@RequestParam(required = false) String code,
                                                  @RequestParam(required = false) String email,
                                                  @RequestParam(required = false) String name) {
        return authService.completeNaverCallback(code, email, name);
    }

    @PostMapping("/dev/login")
    public AuthService.AuthResponse devLogin(@RequestBody DevLoginRequest request) {
        return authService.loginDev(request.email(), request.name());
    }

    @GetMapping("/me")
    public MeResponse me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        var user = authService.requireUser(authorization);
        return new MeResponse(user.getId(), user.getEmail(), user.getName(), user.getProvider().name());
    }

    public record AuthorizeUrlResponse(String url) {
    }

    public record DevLoginRequest(String email, String name) {
    }

    public record MeResponse(Long userId, String email, String name, String provider) {
    }
}

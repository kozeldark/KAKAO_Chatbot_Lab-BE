package com.example.goENC.user.controllers;
import com.example.goENC.user.dto.UserResponseDto;
import com.example.goENC.user.dto.SignupSocialDto;
import com.example.goENC.user.service.KakaoUserService;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
@RequiredArgsConstructor
public class SignUpController {
    private final KakaoUserService kakaoUserService;
    private final String AUTH_HEADER = "Authorization";


    // 카카오 회원가입
    @GetMapping("/user/kakao/callback")
    public SignupSocialDto kakaoLogin(@RequestParam String code,
                                      HttpServletResponse response)
            throws IOException {
        // authorizedCode: 카카오 서버로부터 받은 인가 코드
        SignupSocialDto signupKakaoDto = kakaoUserService.kakaoLogin(code);
        response.addHeader(AUTH_HEADER, signupKakaoDto.getToken());

        return signupKakaoDto;
    }

    // 카카오 프로필 업데이트
    @GetMapping("/user/kakao/callback/{userId}")
    public ResponseEntity<UserResponseDto> kakaoAddUserProfile(@RequestParam String code,
                                                              @PathVariable Long userId
    ) throws IOException {
        return ResponseEntity.ok().body(kakaoUserService.kakaoAddUserProfile(code, userId));
    }
}
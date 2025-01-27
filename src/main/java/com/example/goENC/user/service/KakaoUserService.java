package com.example.goENC.user.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.goENC.user.dto.SignupSocialDto;
import com.example.goENC.user.dto.UserResponseDto;
import com.example.goENC.model.User;
import com.example.goENC.repository.UserRepository;
import com.example.goENC.security.UserDetailsImpl;
import com.example.goENC.security.jwt.JwtTokenUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.UUID;

@RequiredArgsConstructor
@Service //서비스로 등록 -> Bean등록
public class KakaoUserService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    //    @Value("${kakao.client.id}")
    private String clientId;

    @Transactional
    public SignupSocialDto kakaoLogin(String code) throws IOException {
        // 1. "인가 코드"로 "액세스 토큰" 요청
        String accessToken = getAccessToken(code, "http://localhost:3000/login");


        // 2. 필요시에 회원가입
        User kakaoUser = registerKakaoUserIfNeeded(accessToken);

        // 3. 로그인 JWT 토큰 발행
        String token = jwtTokenCreate(kakaoUser);

        UserResponseDto userResponseDto = new UserResponseDto(kakaoUser.getId(), kakaoUser.getUsername(), kakaoUser.getNickname(), kakaoUser.getProfileImg());
        return SignupSocialDto.builder()
                .token(token)
                .userId(kakaoUser.getId())
                .userResponseDto(userResponseDto)
                .build();
    }

    @Transactional
    public UserResponseDto kakaoAddUserProfile(String code, Long userId) throws IOException {
        // 업데이트 필요성 체크
        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("유저조회불가")
        );

        UserResponseDto userLoginResponseDto;
        if (user.getId() == null) {
            // 1. "인가 코드"로 "액세스 토큰" 요청
            String accessToken = getAccessToken(code, "http://localhost:3000/login");



            // 2. 유저 정보 업데이트
            userLoginResponseDto = updateUserProfile(accessToken, user);
        } else {
            userLoginResponseDto = UserResponseDto.builder()
                    .userId(user.getId())
                    .username(user.getUsername())
                    .nickname(user.getNickname())
                    .profileImg(user.getProfileImg())
                    .build();
        }

        return userLoginResponseDto;
    }

    private String getAccessToken(String code, String redirect_uri) throws IOException {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP Body 생성
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("client_id", "bac376255674f663efac55e7ab39fba9");
        body.add("redirect_uri", "http://localhost:3000/login");
//        body.add("redirect_uri", "https://www.hometmate.com/user/kakao/callback");
        body.add("code", code);

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoTokenRequest = new HttpEntity<>(body, headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://kauth.kakao.com/oauth/token",
                HttpMethod.POST,
                kakaoTokenRequest,
                String.class
        );

        // HTTP 응답 (JSON) -> 액세스 토큰 파싱
        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(responseBody);
        return jsonNode.get("access_token").asText();
    }

    // 처음 로그인 시, 회원 가입 안되어 있으면 회원 가입 시켜주기
    private User registerKakaoUserIfNeeded(String accessToken) throws IOException {
        JsonNode jsonNode = getKakaoUserInfo(accessToken);

        // DB 에 중복된 Kakao Id 가 있는지 확인
        String kakaoId = String.valueOf(jsonNode.get("id").asLong());
        User kakaoUser = userRepository.findByUsername(kakaoId).orElse(null);

        // 회원가입
        if (kakaoUser == null) {
            String profileImg = "";
            String kakaoNick = jsonNode.get("properties").get("nickname").asText();
            String password = UUID.randomUUID().toString();
            String encodedPassword = passwordEncoder.encode(password);
            //카카오 프로필 미등록 유저는 homeTmate 기본 프로필 이미지로 등록
            if(jsonNode.get("properties").get("profile_image") == null) {
               profileImg = "https://developers.kakao.com/docs/static/image/ko/m/kakaotalk-social.png";
            } else {
                profileImg = jsonNode.get("properties").get("profile_image").asText();
            }
                kakaoUser = new User(kakaoId, kakaoNick, encodedPassword, profileImg);
            userRepository.save(kakaoUser);
        }

        return kakaoUser;
    }

    // 유저 프로필 등록
    private UserResponseDto updateUserProfile(String accessToken, User user) throws IOException {
        JsonNode jsonNode = getKakaoUserInfo(accessToken);

        return UserResponseDto.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .profileImg(user.getProfileImg())
                .build();
    }

    // 카카오에서 동의 항목 가져오기
    private JsonNode getKakaoUserInfo(String accessToken) throws IOException {
        // HTTP Header 생성
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + accessToken);
        headers.add("Content-type", "application/x-www-form-urlencoded;charset=utf-8");

        // HTTP 요청 보내기
        HttpEntity<MultiValueMap<String, String>> kakaoUserInfoRequest = new HttpEntity<>(headers);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<String> response = rt.exchange(
                "https://kapi.kakao.com/v2/user/me",
                HttpMethod.POST,
                kakaoUserInfoRequest,
                String.class
        );

        String responseBody = response.getBody();
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(responseBody);
    }

    // JWT 토큰 생성
    private String jwtTokenCreate(User kakaoUser) {
        String TOKEN_TYPE = "BEARER";

        UserDetails userDetails = new UserDetailsImpl(kakaoUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails1 = ((UserDetailsImpl) authentication.getPrincipal());
        final String token = JwtTokenUtils.generateJwtToken(userDetails1);
        return TOKEN_TYPE + " " + token;
    }
}
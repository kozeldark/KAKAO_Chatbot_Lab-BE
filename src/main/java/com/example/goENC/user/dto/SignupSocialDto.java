package com.example.goENC.user.dto;

import com.example.goENC.user.dto.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SignupSocialDto {
    String token;
    Long userId;
    UserResponseDto userResponseDto;
}
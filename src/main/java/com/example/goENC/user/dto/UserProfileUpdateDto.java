package com.example.goENC.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileUpdateDto {
    private String nickname;
    private String career;
    private String selfIntro;
}
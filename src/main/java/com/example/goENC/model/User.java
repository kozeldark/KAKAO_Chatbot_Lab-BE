package com.example.goENC.model;

import com.example.goENC.user.dto.UserProfileUpdateDto;
import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false) // 닉네임 중복 허용
    private String nickname;

    @Column(nullable = false)
    private String password;

    @Column
    private String profileImg;


    @Column
    private String email;



    public User(String username, String nickname, String enPassword, String profileImg, String email){
        this.username = username;
        this.nickname = nickname;
        this.password = enPassword;
        this.profileImg = profileImg;
        this.email = email;
    }

    @Builder
    public User(String username, String nickname, String enPassword, String profileImg){
        this.username = username;
        this.nickname = nickname;
        this.password = enPassword;
        this.profileImg = profileImg;
    }

    public void setProfileImg(String imgPath) {
        this.profileImg = imgPath;
    }
}
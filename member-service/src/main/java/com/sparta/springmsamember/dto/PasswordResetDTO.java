package com.sparta.springmsamember.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetDTO {
    private String email;
    private String token;
    private String newPassword;

}
package com.sparta.springmsamember.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class LoginResponseDTO {

    private String accessToken;
    private String refreshToken;
    private String message;

    public LoginResponseDTO(String accessToken, String refreshToken, String message) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.message = message;
    }

}




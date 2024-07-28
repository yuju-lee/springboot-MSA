package com.sparta.springmsamember.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetRequestDTO {
    private String email;
    private String phone;
    private String userName;

}

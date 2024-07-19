package com.sparta.springmsamember.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberRoleResponseDTO {
    private String email;
    private String role;

    public MemberRoleResponseDTO(String email, String role) {
        this.email = email;
        this.role = role;
    }
}
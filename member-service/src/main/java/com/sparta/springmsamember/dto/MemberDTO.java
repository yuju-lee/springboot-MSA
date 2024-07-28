package com.sparta.springmsamember.dto;

import lombok.Getter;
import lombok.Setter;
import org.antlr.v4.runtime.misc.NotNull;

@Getter
@Setter
public class MemberDTO {
    @NotNull
    private String email;

    @NotNull
    private String userName;

    @NotNull
    private String password;

    private String role="ROLE_USER";

}
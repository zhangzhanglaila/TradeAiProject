package com.trade.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;

    @JsonAlias({"uuid", "captchaKey"})
    private String uuid;

    @JsonAlias({"code", "captchaCode"})
    private String code;
}

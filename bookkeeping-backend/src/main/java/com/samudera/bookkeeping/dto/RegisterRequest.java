package com.samudera.bookkeeping.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class RegisterRequest {

    @NotBlank(message = "must not be blank")
    @Size(min = 4, max = 20, message = "length must be between 4 and 20")
    private String username;

    @NotBlank(message = "must not be blank")
    @Size(min = 6, max = 20, message = "length must be between 6 and 20")
    private String password;

    @NotBlank(message = "must not be blank")
    @Size(max = 50, message = "length must be less than or equal to 50")
    private String nickname;

    @Email(message = "must be a valid email")
    private String email;
}
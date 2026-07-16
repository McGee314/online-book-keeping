package com.samudera.bookkeeping.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.Size;

@Data
public class ProfileUpdateRequest {

    @Size(max = 100, message = "Nickname must be at most 100 characters")
    private String nickname;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email must be at most 100 characters")
    private String email;

    @Size(max = 255, message = "Avatar URL must be at most 255 characters")
    private String avatar;
}
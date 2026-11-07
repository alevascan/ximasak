package com.ximasak.backend.dto;

import lombok.Data;

@Data
public class ResetearPasswordRequest {
    private String token;
    private String nuevaPassword;
}
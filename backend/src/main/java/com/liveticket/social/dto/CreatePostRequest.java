package com.liveticket.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreatePostRequest {

    @NotBlank(message = "content is required")
    @Size(max = 1000, message = "content length must be <= 1000")
    private String content;

    private Long eventId;

    private String imageUrl;
}

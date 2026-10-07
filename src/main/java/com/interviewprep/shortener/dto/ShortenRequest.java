package com.interviewprep.shortener.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.hibernate.validator.constraints.URL;

public record ShortenRequest(
        @NotBlank(message = "url is required")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        @URL(regexp = "^(?i)https?://.+", message = "url must be a valid http or https URL")
        String url,

        @Future(message = "expiresAt must be in the future")
        Instant expiresAt) {
}

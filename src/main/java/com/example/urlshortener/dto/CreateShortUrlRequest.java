package com.example.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

/**
 * Request body for {@code POST /api/v1/urls}.
 *
 * <p>Validated automatically before the controller runs, so malformed input is
 * rejected with a 400 and never reaches the service layer.
 *
 * @param url         the long URL to shorten (must be a syntactically valid http/https URL)
 * @param customAlias optional vanity alias; Base62 charset, 3-16 chars
 */
public record CreateShortUrlRequest(

        @NotBlank(message = "url is required")
        @URL(regexp = "^(https?)://.*$", message = "must be a valid http or https URL")
        @Size(max = 2048, message = "url must be at most 2048 characters")
        String url,

        @Pattern(regexp = "^[0-9A-Za-z]{3,16}$",
                message = "customAlias must be 3-16 alphanumeric characters")
        String customAlias
) {
}

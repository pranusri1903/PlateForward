package com.plateforward.listing;

import com.plateforward.listing.Listing.*;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record ListingRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 2000) String description,
        @NotNull Category category,
        @NotNull Storage storage,
        @NotBlank @Size(max = 60) String quantity,
        @NotBlank String area,
        @NotBlank String address,
        @NotNull Audience audience,
        @NotNull Instant pickupStart,
        @NotNull Instant pickupEnd,
        @NotNull Instant expiresAt) {}

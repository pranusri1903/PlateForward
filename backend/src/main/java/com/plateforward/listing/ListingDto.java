package com.plateforward.listing;

import com.plateforward.listing.Feedback.Kind;
import com.plateforward.listing.FeedbackDtos.FeedbackDto;
import com.plateforward.listing.Listing.*;
import java.time.Instant;
import java.util.List;

/**
 * What the current viewer sees. {@code address} and party phones are hidden until the pickup is confirmed,
 * and {@code actions} lists what this viewer may do next, so the frontend never re-implements the rules.
 * {@code feedback} and {@code feedbackKinds} are filled only on the detail view.
 */
public record ListingDto(Long id, String title, String description, Category category, Storage storage, String quantity,
                         String area, String address, Audience audience, Status status,
                         Instant pickupStart, Instant pickupEnd, Instant expiresAt, Instant claimExpiresAt, Instant pickedUpAt, Instant createdAt,
                         Party donor, Party claimer, List<String> actions,
                         List<FeedbackDto> feedback, List<Kind> feedbackKinds) {
    public record Party(Long id, String name, String phone) {}
}

package com.plateforward.listing;

import com.plateforward.listing.Feedback.Kind;
import com.plateforward.user.User;
import jakarta.validation.constraints.*;
import java.time.Instant;

public interface FeedbackDtos {
    record FeedbackRequest(@NotNull Kind kind, @Min(1) @Max(5) Integer rating, @NotBlank @Size(max = 2000) String text) {}

    record ResolveRequest(@NotBlank @Size(max = 2000) String resolution) {}

    record FeedbackDto(Long id, Kind kind, Integer rating, String text, Long authorId, String author, String resolution, Instant createdAt) {
        static FeedbackDto of(Feedback f) {
            return new FeedbackDto(f.getId(), f.getKind(), f.getRating(), f.getText(), f.getAuthor().getId(), f.getAuthor().getName(), f.getResolution(), f.getCreatedAt());
        }
    }

    record Person(Long id, String name, User.Role role) {
        static Person of(User u) {
            return u == null ? null : new Person(u.getId(), u.getName(), u.getRole());
        }
    }

    /** What an admin sees when handling a dispute: who reported whom, about which listing. */
    record DisputeDto(Long id, Kind kind, String text, Instant createdAt, String resolution, Instant resolvedAt,
                      Long listingId, String listingTitle, Person reporter, Person against) {
        static DisputeDto of(Feedback f) {
            var l = f.getListing();
            var reporterIsDonor = f.getAuthor().getId().equals(l.getDonor().getId());
            return new DisputeDto(f.getId(), f.getKind(), f.getText(), f.getCreatedAt(), f.getResolution(), f.getResolvedAt(), l.getId(), l.getTitle(),
                    Person.of(f.getAuthor()), Person.of(reporterIsDonor ? l.getClaimer() : l.getDonor()));
        }
    }
}

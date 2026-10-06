package com.plateforward.listing;

import com.plateforward.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.*;

/** A review (after a pickup) or a report (no-show / issue) that an admin can resolve. */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feedback {
    public enum Kind { REVIEW, NO_SHOW, ISSUE }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Listing listing;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User author;
    @Enumerated(EnumType.STRING)
    private Kind kind;
    private Integer rating;
    private String text, resolution;
    private Instant resolvedAt;
    @Builder.Default
    private Instant createdAt = Instant.now();
}

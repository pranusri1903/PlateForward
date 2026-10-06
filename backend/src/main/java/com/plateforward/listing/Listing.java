package com.plateforward.listing;

import com.plateforward.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "listings")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Listing {
    public enum Category { PRODUCE, BAKERY, PREPARED, DAIRY, MEAT, PANTRY, OTHER }
    public enum Storage { AMBIENT, REFRIGERATED, FROZEN, HOT }
    public enum Audience { ORGS, ANYONE }
    public enum Status { LISTED, CLAIMED, CONFIRMED, PICKED_UP, EXPIRED }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User donor;
    @ManyToOne(fetch = FetchType.LAZY)
    private User claimer;

    private String title, description, quantity, area, address;
    @Enumerated(EnumType.STRING) private Category category;
    @Enumerated(EnumType.STRING) private Storage storage;
    @Enumerated(EnumType.STRING) private Audience audience;
    @Enumerated(EnumType.STRING) @Builder.Default private Status status = Status.LISTED;

    private Instant pickupStart, pickupEnd, expiresAt, claimExpiresAt, pickedUpAt;
    private boolean reminderSent, expiryWarningSent; // each email is sent once per listing
    @Builder.Default
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "listing")
    @OrderBy("id")
    @Builder.Default
    private List<Feedback> feedback = new ArrayList<>();
}

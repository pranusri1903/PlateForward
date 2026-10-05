package com.plateforward.listing;

import com.plateforward.user.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "listings")
@Getter
@Setter
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
    @Enumerated(EnumType.STRING) private Status status = Status.LISTED;

    private Instant pickupStart, pickupEnd, expiresAt, claimExpiresAt, pickedUpAt;
    private Instant createdAt = Instant.now();
}

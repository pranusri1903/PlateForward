package com.plateforward.listing;

import com.plateforward.listing.Listing.*;
import com.plateforward.user.User;
import java.time.Duration;
import java.time.Instant;

/** Builders for the small scenarios the tests need. */
final class TestData {
    private TestData() {}

    static User user(long id, User.Role role, boolean verified) {
        var u = new User();
        u.setId(id);
        u.setEmail("user" + id + "@test.com");
        u.setName(role + " " + id);
        u.setRole(role);
        u.setVerified(verified);
        return u;
    }

    /** A listing (id 1) whose pickup window starts in an hour and which expires in six, open to organizations only. */
    static Listing listing(User donor, Status status) {
        var now = Instant.now();
        return Listing.builder().id(1L).donor(donor).title("Bread").address("1 Main St").audience(Audience.ORGS).status(status)
                .pickupStart(now.plus(Duration.ofHours(1))).pickupEnd(now.plus(Duration.ofHours(3))).expiresAt(now.plus(Duration.ofHours(6))).build();
    }

    static Feedback review(Listing l, User author) {
        return Feedback.builder().listing(l).author(author).kind(Feedback.Kind.REVIEW).rating(5).text("Great").build();
    }
}

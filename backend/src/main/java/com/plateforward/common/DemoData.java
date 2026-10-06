package com.plateforward.common;

import com.plateforward.listing.*;
import com.plateforward.listing.Feedback.Kind;
import com.plateforward.listing.Listing.*;
import com.plateforward.user.User;
import com.plateforward.user.User.Role;
import com.plateforward.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Sample accounts and listings for demos. Enabled with SPRING_PROFILES_ACTIVE=demo; every demo account uses the password "demo1234". */
@Component
@Profile("demo")
@RequiredArgsConstructor
class DemoData implements ApplicationRunner {
    private final UserRepository users;
    private final ListingRepository listings;
    private final FeedbackRepository feedback;
    private final PasswordEncoder encoder;

    @Override
    public void run(ApplicationArguments args) {
        if (listings.count() > 0) return;
        var bakery = user("bakery@demo.com", "Golden Crust Bakery", Role.DONOR, "Downtown", true);
        var bistro = user("bistro@demo.com", "Olive & Thyme Bistro", Role.DONOR, "Riverside", true);
        var market = user("market@demo.com", "Green Basket Market", Role.DONOR, "Eastside", true);
        var maya = user("maya@demo.com", "Maya Patel", Role.GIVER, "Eastside", true);
        var foodbank = user("foodbank@demo.com", "Community Food Bank", Role.ORG, "Downtown", true);
        user("shelter@demo.com", "Hope Shelter", Role.ORG, "Eastside", false); // waiting for admin verification
        user("sam@demo.com", "Sam Rivera", Role.TAKER, "Eastside", true);

        // Open listings
        add(bakery, "Fresh sourdough & pastries", Category.BAKERY, Storage.AMBIENT, "40 loaves", "Downtown", Audience.ORGS, 1, 3, 5, "Baked this morning, packed in paper bags. Bring your own crates.");
        add(bistro, "Tonight's pasta trays", Category.PREPARED, Storage.HOT, "25 meals", "Riverside", Audience.ORGS, 1, 2, 2.5, "Penne arrabbiata in foil trays. Vegetarian, contains gluten.");
        add(market, "Crates of ripe tomatoes", Category.PRODUCE, Storage.REFRIGERATED, "6 crates", "Eastside", Audience.ANYONE, 2, 6, 20, "Slightly soft but perfect for sauces and soups.");
        add(maya, "Homemade lasagna", Category.PREPARED, Storage.REFRIGERATED, "4 portions", "Eastside", Audience.ANYONE, 2, 5, 18, "Made too much for a party. Contains dairy and gluten.");
        add(bakery, "Day-old croissants", Category.BAKERY, Storage.AMBIENT, "30 pieces", "Downtown", Audience.ANYONE, 1, 4, 8, null);
        add(market, "Yogurt & milk close to date", Category.DAIRY, Storage.REFRIGERATED, "48 cartons", "Eastside", Audience.ORGS, 3, 8, 30, "Best-before in 3 days. Cold chain kept.");
        add(bistro, "Canned beans & rice", Category.PANTRY, Storage.AMBIENT, "12 kg", "Riverside", Audience.ORGS, 4, 10, 72, "Unopened catering packs.");

        // A claim waiting for the donor, and a confirmed pickup
        var claimed = add(bakery, "Fresh bagels", Category.BAKERY, Storage.AMBIENT, "60 bagels", "Downtown", Audience.ORGS, 2, 4, 6, "Assorted, baked today.");
        claimed.setStatus(Status.CLAIMED);
        claimed.setClaimer(foodbank);
        claimed.setClaimExpiresAt(Instant.now().plus(Duration.ofMinutes(90)));
        var confirmed = add(bistro, "Vegetable stew", Category.PREPARED, Storage.HOT, "30 meals", "Riverside", Audience.ORGS, 0.5, 2, 4, "Vegan, packed in sealed tubs.");
        confirmed.setStatus(Status.CONFIRMED);
        confirmed.setClaimer(foodbank);

        // History: a finished pickup with reviews from both sides, and an expired one with an open no-show report
        var done = add(bakery, "Yesterday's sandwiches", Category.PREPARED, Storage.REFRIGERATED, "50 sandwiches", "Downtown", Audience.ORGS, -30, -28, -24, null);
        done.setStatus(Status.PICKED_UP);
        done.setClaimer(foodbank);
        done.setPickedUpAt(Instant.now().minus(Duration.ofHours(28)));
        review(done, foodbank, 5, "Fresh, well packed and the handoff took two minutes. Thank you!");
        review(done, bakery, 5, "Punctual and friendly. Happy to donate to them again.");
        var missed = add(bistro, "Soup containers", Category.PREPARED, Storage.HOT, "20 containers", "Riverside", Audience.ORGS, -30, -28, -24, null);
        missed.setStatus(Status.EXPIRED);
        missed.setClaimer(foodbank);
        feedback.save(Feedback.builder().listing(missed).author(bistro).kind(Kind.NO_SHOW).text("Waited an hour past the pickup window and nobody came or called.").build());
        listings.saveAll(java.util.List.of(claimed, confirmed, done, missed));
    }

    private User user(String email, String name, Role role, String area, boolean verified) {
        var u = new User();
        u.setEmail(email);
        u.setName(name);
        u.setRole(role);
        u.setArea(area);
        u.setVerified(verified);
        u.setPhone("555-01" + (10 + users.count()));
        u.setPasswordHash(encoder.encode("demo1234"));
        return users.save(u);
    }

    private Listing add(User donor, String title, Category c, Storage s, String qty, String area, Audience aud, double startH, double endH, double expH, String desc) {
        Instant now = Instant.now();
        return listings.save(Listing.builder().donor(donor).title(title).description(desc).category(c).storage(s).quantity(qty).area(area).address("12 " + area + " St").audience(aud)
                .pickupStart(now.plus(Duration.ofMinutes((long) (startH * 60)))).pickupEnd(now.plus(Duration.ofMinutes((long) (endH * 60)))).expiresAt(now.plus(Duration.ofMinutes((long) (expH * 60)))).build());
    }

    private void review(Listing l, User author, int rating, String text) {
        feedback.save(Feedback.builder().listing(l).author(author).kind(Kind.REVIEW).rating(rating).text(text).build());
    }
}

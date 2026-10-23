package com.plateforward.listing;

import static com.plateforward.listing.TestData.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.plateforward.listing.Feedback.Kind;
import com.plateforward.listing.Listing.*;
import com.plateforward.notify.Notice;
import com.plateforward.user.User;
import com.plateforward.user.User.Role;
import com.plateforward.user.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ListingServiceTest {
    @Mock ListingRepository listings;
    @Mock UserRepository users;
    @Mock ApplicationEventPublisher events;
    @InjectMocks ListingService service;

    final User donor = user(1, Role.DONOR, true);
    final User verifiedOrg = user(2, Role.ORG, true);
    final User unverifiedOrg = user(3, Role.ORG, false);
    final User taker = user(4, Role.TAKER, true);
    final User otherOrg = user(5, Role.ORG, true);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "claimHold", Duration.ofHours(2)); // normally injected from application.properties
    }

    /** Runs {@code action} as {@code actor} on a listing in the given state, with the repositories stubbed accordingly. */
    private Listing act(Listing l, User actor, String action) {
        when(listings.lockById(l.getId())).thenReturn(Optional.of(l));
        when(users.findById(actor.getId())).thenReturn(Optional.of(actor));
        service.act(l.getId(), actor.getId(), action);
        return l;
    }

    private void assertConflict(Listing l, User actor, String action) {
        assertThatThrownBy(() -> act(l, actor, action))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Nested
    class Claiming {
        @Test
        void unverifiedOrganizationCannotClaim() {
            var l = listing(donor, Status.LISTED);
            assertConflict(l, unverifiedOrg, "claim");
            assertThat(l.getStatus()).isEqualTo(Status.LISTED);
            assertThat(l.getClaimer()).isNull();
            verifyNoInteractions(events);
        }

        @Test
        void verifiedOrganizationClaimsAndTheDonorIsNotified() {
            var l = act(listing(donor, Status.LISTED), verifiedOrg, "claim");
            assertThat(l.getStatus()).isEqualTo(Status.CLAIMED);
            assertThat(l.getClaimer()).isSameAs(verifiedOrg);
            assertThat(l.getClaimExpiresAt()).isBetween(Instant.now().plus(Duration.ofMinutes(119)), Instant.now().plus(Duration.ofMinutes(121)));
            verify(events).publishEvent(any(Notice.class));
        }

        @Test
        void takerCannotClaimAListingReservedForOrganizations() {
            var l = listing(donor, Status.LISTED);
            assertConflict(l, taker, "claim");
            assertThat(l.getStatus()).isEqualTo(Status.LISTED);
        }

        @Test
        void takerCanClaimAListingOpenToEveryone() {
            var l = listing(donor, Status.LISTED);
            l.setAudience(Audience.ANYONE);
            assertThat(act(l, taker, "claim").getClaimer()).isSameAs(taker);
        }

        @Test
        void donorCannotClaimTheirOwnListing() {
            assertConflict(listing(donor, Status.LISTED), donor, "claim");
        }

        @Test
        void expiredListingCannotBeClaimed() {
            var l = listing(donor, Status.LISTED);
            l.setExpiresAt(Instant.now().minusSeconds(1));
            assertConflict(l, verifiedOrg, "claim");
        }

        @Test
        void listingThatIsAlreadyClaimedCannotBeClaimedAgain() {
            var l = listing(donor, Status.CLAIMED);
            l.setClaimer(verifiedOrg);
            assertConflict(l, otherOrg, "claim");
            assertThat(l.getClaimer()).isSameAs(verifiedOrg);
        }
    }

    @Nested
    class Confirming {
        private Listing claimed() {
            var l = listing(donor, Status.CLAIMED);
            l.setClaimer(verifiedOrg);
            l.setClaimExpiresAt(Instant.now().plus(Duration.ofHours(1)));
            return l;
        }

        @Test
        void donorConfirmsAndTheHoldTimerClears() {
            var l = act(claimed(), donor, "confirm");
            assertThat(l.getStatus()).isEqualTo(Status.CONFIRMED);
            assertThat(l.getClaimExpiresAt()).isNull();
        }

        @Test
        void theClaimerCannotConfirmTheirOwnClaim() {
            var l = claimed();
            assertConflict(l, verifiedOrg, "confirm");
            assertThat(l.getStatus()).isEqualTo(Status.CLAIMED);
        }

        @Test
        void aStrangerCannotConfirm() {
            assertConflict(claimed(), otherOrg, "confirm");
        }

        @Test
        void onlyTheDonorCanMarkAConfirmedListingPickedUp() {
            var l = claimed();
            l.setStatus(Status.CONFIRMED);
            assertConflict(l, verifiedOrg, "pickup");
            assertThat(act(l, donor, "pickup").getStatus()).isEqualTo(Status.PICKED_UP);
            assertThat(l.getPickedUpAt()).isNotNull();
        }
    }

    @Nested
    class GivingTheListingBack {
        private Listing confirmed() {
            var l = listing(donor, Status.CONFIRMED);
            l.setClaimer(verifiedOrg);
            l.setReminderSent(true);
            return l;
        }

        @Test
        void donorDecliningReturnsItToTheBoard() {
            var l = act(confirmed(), donor, "decline");
            assertThat(l.getStatus()).isEqualTo(Status.LISTED);
            assertThat(l.getClaimer()).isNull();
            assertThat(l.isReminderSent()).as("the next claimer must get a fresh reminder").isFalse();
        }

        @Test
        void claimerReleasingReturnsItToTheBoard() {
            var l = act(confirmed(), verifiedOrg, "release");
            assertThat(l.getStatus()).isEqualTo(Status.LISTED);
            assertThat(l.getClaimer()).isNull();
        }

        @Test
        void claimerCannotDeclineAndDonorCannotRelease() {
            var l = confirmed();
            assertConflict(l, verifiedOrg, "decline");
            assertConflict(l, donor, "release");
            assertThat(l.getStatus()).isEqualTo(Status.CONFIRMED);
        }
    }

    @Nested
    class Deleting {
        @Test
        void donorCanRemoveAnUnclaimedListing() {
            var l = listing(donor, Status.LISTED);
            when(listings.lockById(1L)).thenReturn(Optional.of(l));
            when(users.findById(donor.getId())).thenReturn(Optional.of(donor));
            assertThat(service.act(1L, donor.getId(), "delete")).isNull();
            verify(listings).delete(l);
        }

        @Test
        void aClaimedListingCannotBeRemoved() {
            var l = listing(donor, Status.CLAIMED);
            l.setClaimer(verifiedOrg);
            assertConflict(l, donor, "delete");
            verify(listings, never()).delete(any(Listing.class));
        }
    }

    @Nested
    class Visibility {
        private ListingDto view(Listing l, User viewer) {
            when(listings.findById(1L)).thenReturn(Optional.of(l));
            when(users.findById(viewer.getId())).thenReturn(Optional.of(viewer));
            return service.get(1L, viewer.getId());
        }

        @Test
        void theAddressIsHiddenFromEveryoneButTheDonorUntilConfirmed() {
            var l = listing(donor, Status.CLAIMED);
            l.setClaimer(verifiedOrg);
            assertThat(view(l, verifiedOrg).address()).isNull();
            assertThat(view(l, donor).address()).isEqualTo("1 Main St");
        }

        @Test
        void theClaimerSeesTheAddressOnceConfirmedButStrangersStillDont() {
            var l = listing(donor, Status.CONFIRMED);
            l.setClaimer(verifiedOrg);
            assertThat(view(l, verifiedOrg).address()).isEqualTo("1 Main St");
            assertThat(view(l, otherOrg).address()).isNull();
        }
    }

    @Test
    void aListingMustNotExpireBeforeThePickupWindowCloses() {
        var now = Instant.now();
        var bad = new ListingRequest("Bread", null, Category.BAKERY, Storage.AMBIENT, "5", "Downtown", "1 Main St", Audience.ORGS,
                now.plus(Duration.ofHours(1)), now.plus(Duration.ofHours(3)), now.plus(Duration.ofHours(2)));
        assertThatThrownBy(() -> service.create(bad, donor.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(listings, never()).save(any());
    }

    @Nested
    class FeedbackOptions {
        private Listing confirmed(Status status) {
            var l = listing(donor, status);
            l.setClaimer(verifiedOrg);
            return l;
        }

        @Test
        void aReviewIsOfferedOnlyAfterPickup() {
            assertThat(ListingService.feedbackKinds(confirmed(Status.CONFIRMED), verifiedOrg)).doesNotContain(Kind.REVIEW);
            assertThat(ListingService.feedbackKinds(confirmed(Status.PICKED_UP), verifiedOrg)).contains(Kind.REVIEW);
        }

        @Test
        void aReviewIsOfferedOnlyOncePerPerson() {
            var l = confirmed(Status.PICKED_UP);
            l.getFeedback().add(review(l, verifiedOrg));
            assertThat(ListingService.feedbackKinds(l, verifiedOrg)).containsExactly(Kind.ISSUE);
            assertThat(ListingService.feedbackKinds(l, donor)).as("the other side can still review").contains(Kind.REVIEW);
        }

        @Test
        void aNoShowCannotBeReportedBeforeThePickupWindowOpens() {
            var l = confirmed(Status.CONFIRMED); // window opens in an hour
            assertThat(ListingService.feedbackKinds(l, donor)).containsExactly(Kind.ISSUE);
            l.setPickupStart(Instant.now().minusSeconds(60));
            assertThat(ListingService.feedbackKinds(l, donor)).containsExactly(Kind.NO_SHOW, Kind.ISSUE);
        }

        @Test
        void aCompletedPickupCannotBeReportedAsANoShow() {
            var l = confirmed(Status.PICKED_UP);
            l.setPickupStart(Instant.now().minusSeconds(3600));
            assertThat(ListingService.feedbackKinds(l, donor)).doesNotContain(Kind.NO_SHOW);
        }

        @Test
        void strangersAndUnclaimedListingsGetNoOptions() {
            assertThat(ListingService.feedbackKinds(confirmed(Status.PICKED_UP), otherOrg)).isEmpty();
            assertThat(ListingService.feedbackKinds(listing(donor, Status.EXPIRED), donor)).as("nobody to report").isEmpty();
        }
    }
}

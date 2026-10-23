package com.plateforward.listing;

import static com.plateforward.listing.TestData.listing;
import static com.plateforward.listing.TestData.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.plateforward.listing.Listing.Status;
import com.plateforward.notify.Notice;
import com.plateforward.user.User;
import com.plateforward.user.User.Role;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ListingJobsTest {
    @Mock ListingRepository listings;
    @Mock ApplicationEventPublisher events;
    @InjectMocks ListingJobs jobs;

    final User donor = user(1, Role.DONOR, true);
    final User org = user(2, Role.ORG, true);

    /** The recipients of every email the job published. */
    private List<String> recipients() {
        var sent = ArgumentCaptor.forClass(Notice.class);
        verify(events, atLeast(0)).publishEvent(sent.capture());
        return sent.getAllValues().stream().map(Notice::toEmail).toList();
    }

    @Test
    void anOverdueListingExpiresAndKeepsItsClaimerSoANoShowCanBeReported() {
        var l = listing(donor, Status.CONFIRMED);
        l.setClaimer(org);
        when(listings.lockExpired(any(), any())).thenReturn(List.of(l));

        jobs.run();

        assertThat(l.getStatus()).isEqualTo(Status.EXPIRED);
        assertThat(l.getClaimer()).isSameAs(org);
        assertThat(recipients()).containsExactlyInAnyOrder(donor.getEmail(), org.getEmail());
    }

    @Test
    void anUnclaimedListingThatExpiresOnlyNotifiesTheDonor() {
        var l = listing(donor, Status.LISTED);
        when(listings.lockExpired(any(), any())).thenReturn(List.of(l));

        jobs.run();

        assertThat(l.getStatus()).isEqualTo(Status.EXPIRED);
        assertThat(recipients()).containsExactly(donor.getEmail());
    }

    @Test
    void aClaimTheDonorNeverConfirmedLapsesBackToTheBoard() {
        var l = listing(donor, Status.CLAIMED);
        l.setClaimer(org);
        l.setClaimExpiresAt(Instant.now().minusSeconds(1));
        when(listings.lockLapsedClaims(any())).thenReturn(List.of(l));

        jobs.run();

        assertThat(l.getStatus()).isEqualTo(Status.LISTED);
        assertThat(l.getClaimer()).isNull();
        assertThat(l.getClaimExpiresAt()).isNull();
        assertThat(recipients()).containsExactly(org.getEmail());
    }

    @Test
    void thePickupReminderGoesToBothPartiesAndIsFlaggedSoItIsNotSentAgain() {
        var l = listing(donor, Status.CONFIRMED);
        l.setClaimer(org);
        when(listings.lockNeedingReminder(any())).thenReturn(List.of(l));

        jobs.run();

        assertThat(l.isReminderSent()).as("the repository query skips flagged listings, so this is what makes it once-only").isTrue();
        assertThat(recipients()).containsExactlyInAnyOrder(donor.getEmail(), org.getEmail());
    }

    @Test
    void theExpiryWarningGoesToTheDonorAndIsFlagged() {
        var l = listing(donor, Status.LISTED);
        when(listings.lockNeedingExpiryWarning(any(), any())).thenReturn(List.of(l));

        jobs.run();

        assertThat(l.isExpiryWarningSent()).isTrue();
        assertThat(l.getStatus()).as("a warning must not change the listing").isEqualTo(Status.LISTED);
        assertThat(recipients()).containsExactly(donor.getEmail());
    }

    @Test
    void whenNothingIsDueNothingHappens() {
        jobs.run();
        verifyNoInteractions(events);
    }
}

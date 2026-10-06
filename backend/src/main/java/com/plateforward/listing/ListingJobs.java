package com.plateforward.listing;

import com.plateforward.listing.Listing.Status;
import com.plateforward.notify.Notice;
import com.plateforward.user.User;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Keeps listings honest without anyone clicking: expires them, releases stale claims, and sends reminders. */
@Component
@RequiredArgsConstructor
class ListingJobs {
    private static final Duration REMINDER_LEAD = Duration.ofHours(1), EXPIRY_WARNING_LEAD = Duration.ofHours(3);

    private final ListingRepository listings;
    private final ApplicationEventPublisher events;

    @Scheduled(fixedDelayString = "${app.jobs-interval}")
    @Transactional
    void run() {
        var now = Instant.now();

        for (var l : listings.lockExpired(EnumSet.of(Status.LISTED, Status.CLAIMED, Status.CONFIRMED), now)) {
            l.setStatus(Status.EXPIRED); // the claimer stays on the record so a no-show can still be reported
            tell(l.getDonor(), l, "Expired: " + l.getTitle(), "Your listing expired",
                    l.getClaimer() == null ? "Nobody collected \"%s\" in time.".formatted(l.getTitle()) : "%s didn't collect \"%s\" in time. You can report a no-show.".formatted(l.getClaimer().getName(), l.getTitle()));
            if (l.getClaimer() != null) tell(l.getClaimer(), l, "Missed pickup: " + l.getTitle(), "This pickup expired", "\"%s\" expired before it was collected.".formatted(l.getTitle()));
        }

        for (var l : listings.lockLapsedClaims(now)) {
            tell(l.getClaimer(), l, "Claim lapsed: " + l.getTitle(), "Your claim lapsed", "The donor didn't confirm in time, so \"%s\" is back on the board.".formatted(l.getTitle()));
            l.setStatus(Status.LISTED);
            l.setClaimer(null);
            l.setClaimExpiresAt(null);
        }

        for (var l : listings.lockNeedingReminder(now.plus(REMINDER_LEAD))) {
            for (var who : new User[] {l.getDonor(), l.getClaimer()})
                tell(who, l, "Reminder: pickup soon — " + l.getTitle(), "Your pickup starts soon", "\"%s\" is ready from %s.\nAddress: %s".formatted(l.getTitle(), Notice.time(l.getPickupStart()), l.getAddress()));
            l.setReminderSent(true);
        }

        for (var l : listings.lockNeedingExpiryWarning(now, now.plus(EXPIRY_WARNING_LEAD))) {
            tell(l.getDonor(), l, "About to expire: " + l.getTitle(), "Nobody has claimed this yet", "\"%s\" expires %s. Consider opening it to individuals or posting it again.".formatted(l.getTitle(), Notice.time(l.getExpiresAt())));
            l.setExpiryWarningSent(true);
        }
    }

    private void tell(User to, Listing l, String subject, String heading, String message) {
        events.publishEvent(Notice.to(to, subject, heading, message, "/listings/" + l.getId()));
    }
}

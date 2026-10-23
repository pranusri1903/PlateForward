package com.plateforward;

import static org.assertj.core.api.Assertions.assertThat;

import com.plateforward.notify.Notice;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;

/** The scheduled job against real PostgreSQL, so the actual queries, row locks and "once only" flags are exercised. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {"app.jobs-interval=PT1S", "app.claim-hold=PT3S"})
@Import(BackgroundJobsIntegrationTest.Mailbox.class)
class BackgroundJobsIntegrationTest extends IntegrationTestBase {

    /** Collects every email the application would send. */
    @TestConfiguration
    static class Mailbox {
        final List<Notice> sent = new CopyOnWriteArrayList<>();

        @EventListener
        void on(Notice notice) {
            sent.add(notice);
        }
    }

    @Autowired Mailbox mailbox;

    private static void eventually(String what, BooleanSupplier condition) {
        long deadline = System.nanoTime() + Duration.ofSeconds(20).toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) throw new AssertionError("Timed out waiting for: " + what);
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new AssertionError(e);
            }
        }
    }

    private long emailsAbout(long listingId, String subjectStart) {
        return mailbox.sent.stream().filter(n -> n.path().equals("/listings/" + listingId) && n.subject().startsWith(subjectStart)).count();
    }

    @Test
    void anUnclaimedListingExpiresByItself() {
        var donor = register("DONOR");
        long id = listing(donor.token(), "Short-lived", "ORGS", 0, 1, 2); // expires two seconds from now

        eventually("the listing to expire", () -> status(id, null).equals("EXPIRED"));
        assertThat(emailsAbout(id, "Expired:")).isEqualTo(1);
    }

    @Test
    void aClaimTheDonorNeverConfirmsFallsBackToTheBoard() {
        var donor = register("DONOR");
        var org = verifiedOrg();
        long id = listing(donor.token(), "Never confirmed");
        assertThat(post("/api/listings/" + id + "/claim", org.token(), null).status()).isEqualTo(200);

        eventually("the claim to lapse", () -> status(id, null).equals("LISTED"));
        assertThat(get("/api/listings/" + id, null).<Object>json("$.claimer")).isNull();
        assertThat(emailsAbout(id, "Claim lapsed:")).isEqualTo(1);
        assertThat(post("/api/listings/" + id + "/claim", verifiedOrg().token(), null).status()).as("someone else can now claim it").isEqualTo(200);
    }

    @Test
    void theExpiryWarningIsSentExactlyOnce() throws InterruptedException {
        var donor = register("DONOR");
        long id = listing(donor.token(), "Close to expiry", "ORGS", 60, 120, 2 * 3600); // expires in two hours, inside the 3h warning window

        eventually("the warning", () -> emailsAbout(id, "About to expire:") == 1);
        Thread.sleep(3500); // several more job runs
        assertThat(emailsAbout(id, "About to expire:")).as("the flag stops repeats").isEqualTo(1);
    }

    @Test
    void thePickupReminderGoesToBothPartiesExactlyOnce() throws InterruptedException {
        var donor = register("DONOR");
        var org = verifiedOrg();
        long id = listing(donor.token(), "Reminder test", "ORGS", 30 * 60, 90 * 60, 5 * 3600); // pickup starts in 30 minutes
        var path = "/api/listings/" + id;
        assertThat(post(path + "/claim", org.token(), null).status()).isEqualTo(200);
        assertThat(post(path + "/confirm", donor.token(), null).status()).isEqualTo(200); // inside the 3s claim hold

        eventually("both reminders", () -> emailsAbout(id, "Reminder: pickup soon") == 2);
        Thread.sleep(3500);
        assertThat(emailsAbout(id, "Reminder: pickup soon")).as("one per person, never repeated").isEqualTo(2);
        assertThat(mailbox.sent.stream().filter(n -> n.subject().startsWith("Reminder: pickup soon") && n.path().equals("/listings/" + id)).map(Notice::toName))
                .containsExactlyInAnyOrder("DONOR Test", "ORG Test");
    }
}

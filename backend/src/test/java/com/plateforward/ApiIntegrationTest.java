package com.plateforward;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;

/** The HTTP API against a real PostgreSQL: security rules, the claim race, the whole workflow, and CSV export. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "app.jobs-interval=PT1H") // the background job has its own test class
class ApiIntegrationTest extends IntegrationTestBase {

    @Test
    void flywayMigrationsApplyFromScratch() {
        var applied = jdbc.queryForList("select version from flyway_schema_history where success order by installed_rank", String.class);
        assertThat(applied).containsExactly("1", "2");
    }

    @Test
    void onlyOneOfManyOrganizationsClaimingAtTheSameMomentWins() {
        var donor = register("DONOR");
        var orgs = IntStream.range(0, 6).mapToObj(i -> verifiedOrg()).toList();
        var pool = Executors.newFixedThreadPool(orgs.size());
        try {
            for (int round = 0; round < 5; round++) { // several rounds, so one lucky ordering can't hide a race
                long id = listing(donor.token(), "Race " + round);
                var gate = new CompletableFuture<Void>(); // everyone waits here, then all fire together
                var attempts = orgs.stream().map(o -> gate.thenApplyAsync(v -> Map.entry(o.id(), post("/api/listings/" + id + "/claim", o.token(), null).status()), pool)).toList();
                gate.complete(null);
                var results = attempts.stream().map(CompletableFuture::join).toList();

                var winners = results.stream().filter(e -> e.getValue() == 200).toList();
                assertThat(winners).as("round %d: exactly one claim succeeds", round).hasSize(1);
                assertThat(results.stream().filter(e -> e.getValue() != 200).map(Map.Entry::getValue)).as("everyone else is told it's taken").containsOnly(409);
                var claimerInDb = jdbc.queryForObject("select claimer_id from listings where id = ?", Long.class, id);
                assertThat(claimerInDb).as("the database agrees with the winner").isEqualTo(winners.get(0).getKey());
            }
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void theWholeWorkflowFromListingToPickupAndReviews() {
        var donor = register("DONOR");
        var org = verifiedOrg();
        long id = listing(donor.token(), "Sourdough");
        var path = "/api/listings/" + id;

        assertThat(get(path, org.token()).<List<String>>json("$.actions")).containsExactly("claim");
        assertThat(post(path + "/claim", org.token(), null).status()).isEqualTo(200);
        assertThat(get(path, org.token()).<String>json("$.address")).as("hidden until the donor confirms").isNull();
        assertThat(post(path + "/confirm", org.token(), null).status()).as("the claimer can't confirm their own claim").isEqualTo(409);

        assertThat(post(path + "/confirm", donor.token(), null).status()).isEqualTo(200);
        var asClaimer = get(path, org.token());
        assertThat(asClaimer.<String>json("$.address")).isEqualTo("1 Main St");
        assertThat(asClaimer.<String>json("$.donor.phone")).isEqualTo("555-0100");
        assertThat(get(path, null).<String>json("$.address")).as("guests never see it").isNull();

        assertThat(post(path + "/pickup", donor.token(), null).status()).isEqualTo(200);
        assertThat(status(id, null)).isEqualTo("PICKED_UP");

        assertThat(post(path + "/feedback", org.token(), Map.of("kind", "REVIEW", "text", "Lovely")).status()).as("a review needs a rating").isEqualTo(400);
        assertThat(post(path + "/feedback", org.token(), Map.of("kind", "REVIEW", "rating", 5, "text", "Lovely")).status()).isEqualTo(201);
        assertThat(post(path + "/feedback", org.token(), Map.of("kind", "REVIEW", "rating", 1, "text", "Again")).status()).as("one review each").isEqualTo(409);
        assertThat(post(path + "/feedback", donor.token(), Map.of("kind", "REVIEW", "rating", 4, "text", "Punctual")).status()).isEqualTo(201);
        assertThat(get(path, null).<List<Integer>>json("$.feedback[*].rating")).as("reviews are public").containsExactly(5, 4);

        assertThat(get("/api/listings/history", donor.token()).<List<Integer>>json("$[*].id").stream().map(Number::longValue)).contains(id);
    }

    @Test
    void securityRulesAreEnforcedByTheRealFilterChain() {
        var donor = register("DONOR");
        var org = verifiedOrg();
        var taker = register("TAKER");
        long orgsOnly = listing(donor.token(), "Orgs only");

        assertThat(post("/api/listings", null, Map.of()).status()).as("guests can't post").isEqualTo(401);
        assertThat(get("/api/listings/mine", null).status()).isEqualTo(401);
        assertThat(get("/api/me", "not-a-token").status()).isEqualTo(401);
        assertThat(post("/api/listings", org.token(), Map.of()).status()).as("organizations can't post").isEqualTo(403);
        assertThat(get("/api/admin/organizations", donor.token()).status()).isEqualTo(403);
        assertThat(get("/api/admin/disputes", org.token()).status()).isEqualTo(403);
        assertThat(post("/api/admin/organizations/" + org.id() + "/verify", taker.token(), null).status()).isEqualTo(403);
        assertThat(get("/api/admin/organizations", adminToken()).status()).isEqualTo(200);

        assertThat(post("/api/listings/" + orgsOnly + "/claim", taker.token(), null).status()).as("takers can't claim orgs-only food").isEqualTo(409);
        assertThat(get("/api/listings?size=50", taker.token()).<List<Integer>>json("$.content[*].id").stream().map(Number::longValue)).doesNotContain(orgsOnly);
        assertThat(get("/api/listings?size=50", org.token()).<List<Integer>>json("$.content[*].id").stream().map(Number::longValue)).contains(orgsOnly);

        var unverified = register("ORG");
        assertThat(post("/api/listings/" + orgsOnly + "/claim", unverified.token(), null).status()).as("unverified orgs can't claim").isEqualTo(409);
        assertThat(post("/api/auth/register", null, Map.of("email", "x@test.com", "password", "password1", "name", "x", "role", "ADMIN", "area", "x")).status()).as("no self-service admins").isEqualTo(403);
    }

    @Test
    void aNoShowReportGoesThroughTheAdminQueueToResolution() {
        var donor = register("DONOR");
        var org = verifiedOrg();
        var stranger = verifiedOrg();
        long id = listing(donor.token(), "Never collected", "ORGS", 0, 3600, 7200); // the pickup window is already open
        var path = "/api/listings/" + id;
        assertThat(post(path + "/claim", org.token(), null).status()).isEqualTo(200);
        assertThat(post(path + "/confirm", donor.token(), null).status()).isEqualTo(200);

        assertThat(post(path + "/feedback", stranger.token(), Map.of("kind", "ISSUE", "text", "not mine")).status()).as("only the two parties may report").isEqualTo(409);
        assertThat(post(path + "/feedback", donor.token(), Map.of("kind", "NO_SHOW", "rating", 2, "text", "nobody came")).status()).as("reports don't take a rating").isEqualTo(400);
        assertThat(post(path + "/feedback", donor.token(), Map.of("kind", "NO_SHOW", "text", "")).status()).as("a description is required").isEqualTo(400);
        assertThat(post(path + "/feedback", donor.token(), Map.of("kind", "NO_SHOW", "text", "Waited an hour, nobody came")).status()).isEqualTo(201);

        assertThat(get(path, null).<List<?>>json("$.feedback")).as("guests can't see reports").isEmpty();
        assertThat(get(path, org.token()).<List<String>>json("$.feedback[*].kind")).as("the accused party can").containsExactly("NO_SHOW");

        var admin = adminToken();
        var open = get("/api/admin/disputes", admin).<List<Map<String, Object>>>json("$.open[?(@.listingId == " + id + ")]");
        assertThat(open).hasSize(1);
        assertThat(open.get(0)).containsEntry("kind", "NO_SHOW");
        long disputeId = ((Number) open.get(0).get("id")).longValue();
        assertThat(get("/api/admin/disputes", admin).<List<String>>json("$.open[?(@.id == " + disputeId + ")].reporter.name")).containsExactly("DONOR Test");
        assertThat(get("/api/admin/disputes", admin).<List<String>>json("$.open[?(@.id == " + disputeId + ")].against.name")).containsExactly("ORG Test");

        assertThat(post("/api/admin/disputes/" + disputeId + "/resolve", admin, Map.of("resolution", "")).status()).as("a resolution note is required").isEqualTo(400);
        assertThat(post("/api/admin/disputes/" + disputeId + "/resolve", org.token(), Map.of("resolution", "x")).status()).as("admins only").isEqualTo(403);
        assertThat(post("/api/admin/disputes/" + disputeId + "/resolve", admin, Map.of("resolution", "Warned the organization.")).status()).isEqualTo(200);
        assertThat(post("/api/admin/disputes/" + disputeId + "/resolve", admin, Map.of("resolution", "again")).status()).as("can't resolve twice").isEqualTo(409);

        var after = get("/api/admin/disputes", admin);
        assertThat(after.<List<?>>json("$.open[?(@.id == " + disputeId + ")]")).isEmpty();
        assertThat(after.<List<String>>json("$.resolved[?(@.id == " + disputeId + ")].resolution")).containsExactly("Warned the organization.");
        assertThat(get(path, org.token()).<String>json("$.feedback[0].resolution")).as("both parties see the outcome").isEqualTo("Warned the organization.");
    }

    @Test
    void reviewsAreNotDisputesSoAdminsCannotResolveThem() {
        var donor = register("DONOR");
        var org = verifiedOrg();
        long id = listing(donor.token(), "Reviewed");
        post("/api/listings/" + id + "/claim", org.token(), null);
        post("/api/listings/" + id + "/confirm", donor.token(), null);
        post("/api/listings/" + id + "/pickup", donor.token(), null);
        var review = post("/api/listings/" + id + "/feedback", org.token(), Map.of("kind", "REVIEW", "rating", 5, "text", "Great"));
        assertThat(review.status()).isEqualTo(201);

        assertThat(post("/api/admin/disputes/" + review.id("$.id") + "/resolve", adminToken(), Map.of("resolution", "x")).status()).isEqualTo(404);
        assertThat(post("/api/admin/disputes/999999/resolve", adminToken(), Map.of("resolution", "x")).status()).isEqualTo(404);
    }

    @Test
    void registrationRejectsBadInputWithAFieldMap() {
        var r = post("/api/auth/register", null, Map.of("email", "nope", "password", "short", "name", "", "role", "DONOR", "area", ""));
        assertThat(r.status()).isEqualTo(400);
        assertThat(r.<Map<String, String>>json("$.errors")).containsKeys("email", "password", "name", "area");
    }

    @Test
    void csvExportQuotesCommasAndNeutralizesSpreadsheetFormulas() {
        var donor = register("DONOR");
        var org = verifiedOrg();
        for (var title : List.of("He said \"hi\", ok", "=HYPERLINK(\"http://evil\")")) {
            long id = listing(donor.token(), title);
            post("/api/listings/" + id + "/claim", org.token(), null);
            post("/api/listings/" + id + "/confirm", donor.token(), null);
            post("/api/listings/" + id + "/pickup", donor.token(), null);
        }

        var csv = get("/api/listings/history.csv", donor.token());
        assertThat(csv.status()).isEqualTo(200);
        assertThat(csv.body()).startsWith("id,title,category,quantity,status,donor,collected_by,pickup_start,picked_up_at");
        assertThat(csv.body()).as("commas and quotes survive").contains("\"He said \"\"hi\"\", ok\"");
        assertThat(csv.body()).as("formulas are defused").contains("\"'=HYPERLINK(");
        assertThat(csv.body()).doesNotContain(",=HYPERLINK").doesNotContain(",\"=HYPERLINK");
        assertThat(call(HttpMethod.GET, "/api/listings/history.csv", null, null).status()).isEqualTo(401);
    }
}

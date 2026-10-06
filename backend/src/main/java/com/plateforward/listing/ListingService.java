package com.plateforward.listing;

import static org.springframework.http.HttpStatus.*;

import com.plateforward.listing.Feedback.Kind;
import com.plateforward.listing.FeedbackDtos.FeedbackDto;
import com.plateforward.listing.Listing.*;
import com.plateforward.listing.ListingDto.Party;
import com.plateforward.notify.Notice;
import com.plateforward.user.User;
import com.plateforward.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ListingService {
    private final ListingRepository listings;
    private final UserRepository users;
    private final ApplicationEventPublisher events;
    @Value("${app.claim-hold}")
    private Duration claimHold;

    @Transactional(readOnly = true)
    public PagedModel<ListingDto> search(String q, Category category, Storage storage, boolean newest, int page, int size, Long userId) {
        var viewer = viewer(userId);
        boolean takerOnly = viewer != null && viewer.getRole() == User.Role.TAKER; // individuals only see listings open to them
        Specification<Listing> spec = (root, query, cb) -> {
            var p = new ArrayList<Predicate>();
            p.add(cb.equal(root.get("status"), Status.LISTED));
            p.add(cb.greaterThan(root.get("expiresAt"), Instant.now()));
            if (q != null && !q.isBlank()) {
                var like = "%" + q.toLowerCase() + "%";
                p.add(cb.or(cb.like(cb.lower(root.get("title")), like), cb.like(cb.lower(root.get("area")), like)));
            }
            if (category != null) p.add(cb.equal(root.get("category"), category));
            if (storage != null) p.add(cb.equal(root.get("storage"), storage));
            if (takerOnly) p.add(cb.equal(root.get("audience"), Audience.ANYONE));
            return cb.and(p.toArray(Predicate[]::new));
        };
        var sort = newest ? Sort.by("createdAt").descending() : Sort.by("expiresAt");
        return new PagedModel<>(listings.findAll(spec, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50), sort)).map(l -> dto(l, viewer, false)));
    }

    @Transactional(readOnly = true)
    public ListingDto get(Long id, Long userId) {
        return dto(find(id), viewer(userId), true);
    }

    @Transactional(readOnly = true)
    public List<ListingDto> mine(Long userId) {
        var me = viewer(userId);
        var rows = me.canGive() ? listings.findByDonorIdOrderByPickupStartDesc(userId) : listings.findByClaimerIdOrderByPickupStartDesc(userId);
        return rows.stream().map(l -> dto(l, me, false)).toList();
    }

    /** Finished listings (picked up or expired) for the pickup history page and CSV export. */
    @Transactional(readOnly = true)
    public List<ListingDto> history(Long userId) {
        var me = viewer(userId);
        var done = EnumSet.of(Status.PICKED_UP, Status.EXPIRED);
        var rows = me.canGive() ? listings.findByDonorIdAndStatusInOrderByPickupStartDesc(userId, done) : listings.findByClaimerIdAndStatusInOrderByPickupStartDesc(userId, done);
        return rows.stream().map(l -> dto(l, me, false)).toList();
    }

    @Transactional
    public ListingDto create(ListingRequest r, Long userId) {
        if (!r.pickupStart().isBefore(r.pickupEnd()) || r.expiresAt().isBefore(r.pickupEnd()) || !r.expiresAt().isAfter(Instant.now()))
            throw new ResponseStatusException(BAD_REQUEST, "Pickup must start before it ends, and the food must not expire before the pickup window closes");
        var donor = viewer(userId);
        var l = listings.save(Listing.builder().donor(donor).title(r.title()).description(r.description()).category(r.category()).storage(r.storage())
                .quantity(r.quantity()).area(r.area()).address(r.address()).audience(r.audience())
                .pickupStart(r.pickupStart()).pickupEnd(r.pickupEnd()).expiresAt(r.expiresAt()).build());

        // Tell verified organizations (and individuals, if the listing is open to them) in the same neighborhood
        var audience = r.audience() == Audience.ANYONE ? List.of(User.Role.ORG, User.Role.TAKER) : List.of(User.Role.ORG);
        users.findByRoleInAndVerifiedTrueAndAreaIgnoreCase(audience, r.area()).forEach(u -> tell(u, l, "New surplus food nearby: " + l.getTitle(), "Food is available in " + l.getArea(),
                "%s from %s is ready to collect (%s). Pickup %s – %s. Claim it before someone else does!".formatted(l.getQuantity(), donor.getName(), l.getTitle(), Notice.time(l.getPickupStart()), Notice.time(l.getPickupEnd()))));
        return dto(l, donor, false);
    }

    /** Runs one workflow step (claim, confirm, decline, release, pickup, delete) under a row lock. Returns null after delete. */
    @Transactional
    public ListingDto act(Long id, Long userId, String action) {
        var l = listings.lockById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Listing not found"));
        var u = viewer(userId);
        if (!actions(l, u).contains(action))
            throw new ResponseStatusException(CONFLICT, "That action isn't available for this listing right now");
        switch (action) {
            case "claim" -> {
                l.setStatus(Status.CLAIMED);
                l.setClaimer(u);
                l.setClaimExpiresAt(Instant.now().plus(claimHold));
                tell(l.getDonor(), l, u.getName() + " claimed " + l.getTitle(), "Someone wants your food",
                        "%s would like to collect \"%s\". Please confirm by %s, or it returns to the board.".formatted(u.getName(), l.getTitle(), Notice.time(l.getClaimExpiresAt())));
            }
            case "confirm" -> {
                l.setStatus(Status.CONFIRMED);
                l.setClaimExpiresAt(null);
                tell(l.getClaimer(), l, "Confirmed: " + l.getTitle(), "Your pickup is confirmed",
                        "Collect \"%s\" between %s and %s.\nAddress: %s".formatted(l.getTitle(), Notice.time(l.getPickupStart()), Notice.time(l.getPickupEnd()), l.getAddress()));
            }
            case "decline" -> {
                tell(l.getClaimer(), l, "Claim declined: " + l.getTitle(), "Your claim was declined", "The donor can't hand over \"%s\" right now. It's back on the board.".formatted(l.getTitle()));
                reopen(l);
            }
            case "release" -> {
                tell(l.getDonor(), l, "Claim released: " + l.getTitle(), "The claimer can no longer collect", "%s released \"%s\". It's available to others again.".formatted(u.getName(), l.getTitle()));
                reopen(l);
            }
            case "pickup" -> {
                l.setStatus(Status.PICKED_UP);
                l.setPickedUpAt(Instant.now());
                tell(l.getClaimer(), l, "Pickup complete: " + l.getTitle(), "Thanks for collecting!", "How did it go? Leave a quick review for %s.".formatted(l.getDonor().getName()));
            }
            case "delete" -> {
                listings.delete(l);
                return null;
            }
        }
        return dto(l, u, true);
    }

    private void reopen(Listing l) {
        l.setStatus(Status.LISTED);
        l.setClaimer(null);
        l.setClaimExpiresAt(null);
        l.setReminderSent(false);
    }

    private void tell(User to, Listing l, String subject, String heading, String message) {
        events.publishEvent(Notice.to(to, subject, heading, message, "/listings/" + l.getId()));
    }

    private Listing find(Long id) {
        return listings.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Listing not found"));
    }

    private User viewer(Long userId) {
        return userId == null ? null : users.findById(userId).orElse(null);
    }

    static boolean same(User a, User b) {
        return a != null && b != null && a.getId().equals(b.getId());
    }

    private static boolean canClaim(Listing l, User u) {
        return l.getExpiresAt().isAfter(Instant.now())
                && ((u.getRole() == User.Role.ORG && u.isVerified()) || (u.getRole() == User.Role.TAKER && l.getAudience() == Audience.ANYONE));
    }

    /** The single source of truth for who may do what at each status. */
    private static List<String> actions(Listing l, User u) {
        if (u == null) return List.of();
        boolean donor = same(l.getDonor(), u), claimer = same(l.getClaimer(), u);
        return switch (l.getStatus()) {
            case LISTED -> donor ? List.of("delete") : canClaim(l, u) ? List.of("claim") : List.of();
            case CLAIMED -> donor ? List.of("confirm", "decline") : claimer ? List.of("release") : List.of();
            case CONFIRMED -> donor ? List.of("pickup", "decline") : claimer ? List.of("release") : List.of();
            default -> List.of();
        };
    }

    /** What feedback the user may still submit: one review after a pickup, no-show reports once the window opened, issues any time after confirming. */
    static List<Kind> feedbackKinds(Listing l, User u) {
        boolean party = same(l.getDonor(), u) || same(l.getClaimer(), u);
        if (!party || l.getClaimer() == null || !List.of(Status.CONFIRMED, Status.PICKED_UP, Status.EXPIRED).contains(l.getStatus())) return List.of();
        var kinds = new ArrayList<Kind>();
        if (l.getStatus() == Status.PICKED_UP && l.getFeedback().stream().noneMatch(f -> f.getKind() == Kind.REVIEW && same(f.getAuthor(), u))) kinds.add(Kind.REVIEW);
        if (l.getStatus() != Status.PICKED_UP && Instant.now().isAfter(l.getPickupStart())) kinds.add(Kind.NO_SHOW);
        kinds.add(Kind.ISSUE);
        return kinds;
    }

    private static ListingDto dto(Listing l, User v, boolean detail) {
        boolean donor = same(l.getDonor(), v), party = donor || same(l.getClaimer(), v);
        boolean contact = party && (l.getStatus() == Status.CONFIRMED || l.getStatus() == Status.PICKED_UP);
        boolean admin = v != null && v.getRole() == User.Role.ADMIN;
        // Reviews are public; reports are visible only to the two parties and admins. Lists skip feedback entirely.
        var feedback = !detail ? List.<FeedbackDto>of() : l.getFeedback().stream().filter(f -> f.getKind() == Kind.REVIEW || party || admin).map(FeedbackDto::of).toList();
        return new ListingDto(l.getId(), l.getTitle(), l.getDescription(), l.getCategory(), l.getStorage(), l.getQuantity(), l.getArea(),
                donor || contact ? l.getAddress() : null, l.getAudience(), l.getStatus(),
                l.getPickupStart(), l.getPickupEnd(), l.getExpiresAt(), l.getClaimExpiresAt(), l.getPickedUpAt(), l.getCreatedAt(),
                party(l.getDonor(), contact), party(l.getClaimer(), contact), actions(l, v), feedback, detail ? feedbackKinds(l, v) : List.of());
    }

    private static Party party(User u, boolean withPhone) {
        return u == null ? null : new Party(u.getId(), u.getName(), withPhone ? u.getPhone() : null);
    }
}

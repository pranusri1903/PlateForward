package com.plateforward.listing;

import static org.springframework.http.HttpStatus.*;

import com.plateforward.listing.Listing.*;
import com.plateforward.listing.ListingDto.Party;
import com.plateforward.user.User;
import com.plateforward.user.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
        return new PagedModel<>(listings.findAll(spec, PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 50), sort)).map(l -> dto(l, viewer)));
    }

    @Transactional(readOnly = true)
    public ListingDto get(Long id, Long userId) {
        return dto(find(id), viewer(userId));
    }

    @Transactional(readOnly = true)
    public List<ListingDto> mine(Long userId) {
        var me = viewer(userId);
        var rows = me.canGive() ? listings.findByDonorIdOrderByPickupStartDesc(userId) : listings.findByClaimerIdOrderByPickupStartDesc(userId);
        return rows.stream().map(l -> dto(l, me)).toList();
    }

    @Transactional
    public ListingDto create(ListingRequest r, Long userId) {
        if (!r.pickupStart().isBefore(r.pickupEnd()) || r.expiresAt().isBefore(r.pickupEnd()) || !r.expiresAt().isAfter(Instant.now()))
            throw new ResponseStatusException(BAD_REQUEST, "Pickup must start before it ends, and the food must not expire before the pickup window closes");
        var l = new Listing();
        l.setDonor(viewer(userId));
        l.setTitle(r.title());
        l.setDescription(r.description());
        l.setCategory(r.category());
        l.setStorage(r.storage());
        l.setQuantity(r.quantity());
        l.setArea(r.area());
        l.setAddress(r.address());
        l.setAudience(r.audience());
        l.setPickupStart(r.pickupStart());
        l.setPickupEnd(r.pickupEnd());
        l.setExpiresAt(r.expiresAt());
        return dto(listings.save(l), l.getDonor());
    }

    /** Runs one workflow step (claim, confirm, decline, release, pickup, delete) under a row lock. Returns null after delete. */
    @Transactional
    public ListingDto act(Long id, Long userId, String action) {
        var l = listings.lockById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Listing not found"));
        var u = viewer(userId);
        if (!actions(l, u).contains(action))
            throw new ResponseStatusException(CONFLICT, "That action isn't available for this listing right now");
        switch (action) {
            case "claim" -> { l.setStatus(Status.CLAIMED); l.setClaimer(u); l.setClaimExpiresAt(Instant.now().plus(claimHold)); }
            case "confirm" -> { l.setStatus(Status.CONFIRMED); l.setClaimExpiresAt(null); }
            case "decline", "release" -> { l.setStatus(Status.LISTED); l.setClaimer(null); l.setClaimExpiresAt(null); }
            case "pickup" -> { l.setStatus(Status.PICKED_UP); l.setPickedUpAt(Instant.now()); }
            case "delete" -> { listings.delete(l); return null; }
        }
        return dto(l, u);
    }

    private Listing find(Long id) {
        return listings.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Listing not found"));
    }

    private User viewer(Long userId) {
        return userId == null ? null : users.findById(userId).orElse(null);
    }

    private static boolean same(User a, User b) {
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

    private static ListingDto dto(Listing l, User v) {
        boolean donor = same(l.getDonor(), v);
        boolean contact = (donor || same(l.getClaimer(), v)) && (l.getStatus() == Status.CONFIRMED || l.getStatus() == Status.PICKED_UP);
        return new ListingDto(l.getId(), l.getTitle(), l.getDescription(), l.getCategory(), l.getStorage(), l.getQuantity(), l.getArea(),
                donor || contact ? l.getAddress() : null, l.getAudience(), l.getStatus(),
                l.getPickupStart(), l.getPickupEnd(), l.getExpiresAt(), l.getClaimExpiresAt(), l.getPickedUpAt(), l.getCreatedAt(),
                party(l.getDonor(), contact), party(l.getClaimer(), contact), actions(l, v));
    }

    private static Party party(User u, boolean withPhone) {
        return u == null ? null : new Party(u.getId(), u.getName(), withPhone ? u.getPhone() : null);
    }
}

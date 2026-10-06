package com.plateforward.listing;

import static org.springframework.http.HttpStatus.*;

import com.plateforward.listing.Feedback.Kind;
import com.plateforward.listing.FeedbackDtos.*;
import com.plateforward.notify.Notice;
import com.plateforward.user.User;
import com.plateforward.user.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class FeedbackService {
    public record Disputes(List<DisputeDto> open, List<DisputeDto> resolved) {}

    private final FeedbackRepository feedback;
    private final ListingRepository listings;
    private final UserRepository users;
    private final ApplicationEventPublisher events;

    @Transactional
    public FeedbackDto submit(Long listingId, Long userId, FeedbackRequest r) {
        var l = listings.findById(listingId).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Listing not found"));
        var author = users.findById(userId).orElseThrow();
        if (!ListingService.feedbackKinds(l, author).contains(r.kind()))
            throw new ResponseStatusException(CONFLICT, "You can't submit that for this listing right now");
        if ((r.kind() == Kind.REVIEW) != (r.rating() != null))
            throw new ResponseStatusException(BAD_REQUEST, "Reviews need a star rating, and reports don't take one");
        Feedback saved;
        try {
            saved = feedback.saveAndFlush(Feedback.builder().listing(l).author(author).kind(r.kind()).rating(r.rating()).text(r.text().strip()).build());
        } catch (DataIntegrityViolationException e) { // a double-click submitting the same review twice
            throw new ResponseStatusException(CONFLICT, "You've already reviewed this pickup");
        }

        var path = "/listings/" + l.getId();
        if (r.kind() == Kind.REVIEW) {
            var other = ListingService.same(l.getDonor(), author) ? l.getClaimer() : l.getDonor();
            events.publishEvent(Notice.to(other, "New review for " + l.getTitle(), "You received a " + r.rating() + "-star review", "%s: \"%s\"".formatted(author.getName(), saved.getText()), path));
        } else {
            var what = r.kind() == Kind.NO_SHOW ? "no-show" : "issue";
            users.findByRole(User.Role.ADMIN).forEach(a -> events.publishEvent(Notice.to(a, "Dispute reported: " + l.getTitle(), "New " + what + " report",
                    "%s reported a %s on \"%s\": %s".formatted(author.getName(), what, l.getTitle(), saved.getText()), "/admin")));
        }
        return FeedbackDto.of(saved);
    }

    @Transactional(readOnly = true)
    public Disputes disputes() {
        return new Disputes(feedback.findByKindNotAndResolutionIsNullOrderByCreatedAtAsc(Kind.REVIEW).stream().map(DisputeDto::of).toList(),
                feedback.findByKindNotAndResolutionIsNotNullOrderByResolvedAtDesc(Kind.REVIEW).stream().map(DisputeDto::of).toList());
    }

    @Transactional
    public DisputeDto resolve(Long id, String resolution) {
        var f = feedback.findById(id).filter(x -> x.getKind() != Kind.REVIEW).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Report not found"));
        if (f.getResolution() != null) throw new ResponseStatusException(CONFLICT, "This report is already resolved");
        f.setResolution(resolution.strip());
        f.setResolvedAt(Instant.now());
        var dto = DisputeDto.of(f);
        var l = f.getListing();
        var other = ListingService.same(l.getDonor(), f.getAuthor()) ? l.getClaimer() : l.getDonor();
        for (var u : List.of(f.getAuthor(), other))
            events.publishEvent(Notice.to(u, "Report resolved: " + l.getTitle(), "A report was resolved", "Our team reviewed the report about \"%s\":\n%s".formatted(l.getTitle(), f.getResolution()), "/listings/" + l.getId()));
        return dto;
    }
}

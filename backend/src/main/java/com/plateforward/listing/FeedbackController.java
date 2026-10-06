package com.plateforward.listing;

import com.plateforward.listing.FeedbackDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
class FeedbackController {
    private final FeedbackService service;

    @PostMapping("/api/listings/{id}/feedback")
    @ResponseStatus(HttpStatus.CREATED)
    FeedbackDto submit(@PathVariable Long id, @Valid @RequestBody FeedbackRequest r, @AuthenticationPrincipal Jwt jwt) {
        return service.submit(id, Long.valueOf(jwt.getSubject()), r);
    }

    @GetMapping("/api/admin/disputes")
    FeedbackService.Disputes disputes() {
        return service.disputes();
    }

    @PostMapping("/api/admin/disputes/{id}/resolve")
    DisputeDto resolve(@PathVariable Long id, @Valid @RequestBody ResolveRequest r) {
        return service.resolve(id, r.resolution());
    }
}

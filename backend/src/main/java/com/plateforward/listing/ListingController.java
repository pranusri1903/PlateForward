package com.plateforward.listing;

import com.plateforward.listing.Listing.*;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {
    private final ListingService service;

    @GetMapping
    PagedModel<ListingDto> search(@RequestParam(required = false) String q, @RequestParam(required = false) Category category,
                                  @RequestParam(required = false) Storage storage, @RequestParam(defaultValue = "expiring") String sort,
                                  @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size,
                                  @AuthenticationPrincipal Jwt jwt) {
        return service.search(q, category, storage, sort.equals("newest"), page, size, uid(jwt));
    }

    @GetMapping("/mine")
    List<ListingDto> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.mine(uid(jwt));
    }

    @GetMapping("/{id}")
    ListingDto get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.get(id, uid(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ListingDto create(@Valid @RequestBody ListingRequest r, @AuthenticationPrincipal Jwt jwt) {
        return service.create(r, uid(jwt));
    }

    /** action: claim | confirm | decline | release | pickup */
    @PostMapping("/{id}/{action}")
    ListingDto act(@PathVariable Long id, @PathVariable String action, @AuthenticationPrincipal Jwt jwt) {
        return service.act(id, uid(jwt), action);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.act(id, uid(jwt), "delete");
    }

    private static Long uid(Jwt jwt) {
        return jwt == null ? null : Long.valueOf(jwt.getSubject());
    }
}

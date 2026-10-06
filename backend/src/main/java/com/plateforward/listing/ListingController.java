package com.plateforward.listing;

import com.plateforward.listing.Listing.*;
import jakarta.validation.Valid;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @GetMapping("/history")
    List<ListingDto> history(@AuthenticationPrincipal Jwt jwt) {
        return service.history(uid(jwt));
    }

    @GetMapping("/history.csv")
    ResponseEntity<String> historyCsv(@AuthenticationPrincipal Jwt jwt) throws IOException {
        var out = new StringWriter();
        try (var csv = new CSVPrinter(out, CSVFormat.DEFAULT)) {
            csv.printRecord("id", "title", "category", "quantity", "status", "donor", "collected_by", "pickup_start", "picked_up_at");
            for (var l : service.history(uid(jwt)))
                csv.printRecord(l.id(), safe(l.title()), l.category(), safe(l.quantity()), l.status(), safe(l.donor().name()), l.claimer() == null ? null : safe(l.claimer().name()), l.pickupStart(), l.pickedUpAt());
        }
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=plateforward-history.csv")
                .contentType(new MediaType("text", "csv")).body(out.toString());
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

    /** Spreadsheets run cells that start with = + - @ as formulas, and titles are user-supplied, so neutralize them. */
    private static String safe(String s) {
        return s.matches("(?s)^[=+\\-@\\t\\r].*") ? "'" + s : s;
    }

    private static Long uid(Jwt jwt) {
        return jwt == null ? null : Long.valueOf(jwt.getSubject());
    }
}

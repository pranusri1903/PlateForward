package com.plateforward.listing;

import com.plateforward.listing.Listing.Status;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
    /** Row lock: concurrent claims on the same listing queue up, so only the first one wins. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Listing l where l.id = :id")
    Optional<Listing> lockById(Long id);

    List<Listing> findByDonorIdOrderByPickupStartDesc(Long donorId);
    List<Listing> findByClaimerIdOrderByPickupStartDesc(Long claimerId);
    List<Listing> findByDonorIdAndStatusInOrderByPickupStartDesc(Long donorId, Collection<Status> statuses);
    List<Listing> findByClaimerIdAndStatusInOrderByPickupStartDesc(Long claimerId, Collection<Status> statuses);

    // Queries for the background job. SKIP LOCKED (-2) leaves rows a user is acting on right now for the next run.
    String SKIP_LOCKED = "-2";

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = SKIP_LOCKED))
    @Query("select l from Listing l where l.status in :open and l.expiresAt <= :now")
    List<Listing> lockExpired(Collection<Status> open, Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = SKIP_LOCKED))
    @Query("select l from Listing l where l.status = 'CLAIMED' and l.claimExpiresAt <= :now")
    List<Listing> lockLapsedClaims(Instant now);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = SKIP_LOCKED))
    @Query("select l from Listing l where l.status = 'CONFIRMED' and l.reminderSent = false and l.pickupStart <= :before")
    List<Listing> lockNeedingReminder(Instant before);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = SKIP_LOCKED))
    @Query("select l from Listing l where l.status = 'LISTED' and l.expiryWarningSent = false and l.expiresAt <= :before and l.expiresAt > :now")
    List<Listing> lockNeedingExpiryWarning(Instant now, Instant before);
}

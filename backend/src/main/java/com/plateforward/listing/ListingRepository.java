package com.plateforward.listing;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ListingRepository extends JpaRepository<Listing, Long>, JpaSpecificationExecutor<Listing> {
    /** Row lock: concurrent claims on the same listing queue up, so only the first one wins. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Listing l where l.id = :id")
    Optional<Listing> lockById(Long id);

    List<Listing> findByDonorIdOrderByPickupStartDesc(Long donorId);
    List<Listing> findByClaimerIdOrderByPickupStartDesc(Long claimerId);
}

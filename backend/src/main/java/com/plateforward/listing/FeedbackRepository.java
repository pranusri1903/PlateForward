package com.plateforward.listing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findByKindNotAndResolutionIsNullOrderByCreatedAtAsc(Feedback.Kind kind);
    List<Feedback> findByKindNotAndResolutionIsNotNullOrderByResolvedAtDesc(Feedback.Kind kind);
}

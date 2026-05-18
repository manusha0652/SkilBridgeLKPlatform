package com.skillbridge.repository;

import com.skillbridge.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByJobRequestId(Long jobRequestId);
    List<Bid> findByFreelancerId(Long freelancerId);
    boolean existsByJobRequestIdAndFreelancerId(Long jobRequestId, Long freelancerId);
}

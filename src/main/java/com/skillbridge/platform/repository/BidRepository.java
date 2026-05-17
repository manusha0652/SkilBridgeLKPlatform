package com.skillbridge.platform.repository;

import com.skillbridge.platform.model.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    // Custom finder to pull all application pitches for a single job post
    List<Bid> findByJobRequestId(Long jobRequestId);
    
    // Custom finder to show a freelancer all the pitches they've submitted
    List<Bid> findByFreelancerId(Long freelancerId);
}
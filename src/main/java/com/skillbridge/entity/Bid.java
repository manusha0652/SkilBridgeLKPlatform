package com.skillbridge.entity;

import com.skillbridge.enums.BidStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_request_id")
    private JobRequest jobRequest;

    @ManyToOne
    @JoinColumn(name = "freelancer_id")
    private User freelancer;

    @Column(name = "rate_lkr", precision = 10, scale = 2, nullable = false)
    private BigDecimal rateLkr;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String proposal;

    @Column(name = "proposed_duration_days")
    private Integer proposedDurationDays;

    @Enumerated(EnumType.STRING)
    private BidStatus status = BidStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }
}

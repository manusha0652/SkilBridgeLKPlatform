package com.skillbridge.entity;

import com.skillbridge.enums.JobStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "job_requests")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class JobRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private User client;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private String category;

    @Column(name = "budget_lkr", precision = 10, scale = 2, nullable = false)
    private BigDecimal budgetLkr;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    @Column(name = "accepted_bid_id")
    private Long acceptedBidId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = JobStatus.OPEN;
    }
}

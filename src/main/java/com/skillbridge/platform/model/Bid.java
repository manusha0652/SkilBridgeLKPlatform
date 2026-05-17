package com.skillbridge.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Direct relationship back to the job request being applied for
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_request_id", nullable = false)
    private JobRequest jobRequest;

    // Direct relationship to the freelancer who is placing the bid
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private User freelancer;

    @Column(nullable = false)
    private Double rate; // The amount the freelancer is bidding

    @Column(columnDefinition = "TEXT", nullable = false)
    private String proposal; // Cover letter or details about their pitch

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // --- Constructors ---
    public Bid() {}

    public Bid(JobRequest jobRequest, User freelancer, Double rate, String proposal) {
        this.jobRequest = jobRequest;
        this.freelancer = freelancer;
        this.rate = rate;
        this.proposal = proposal;
    }

    // --- Getters & Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public JobRequest getJobRequest() { return jobRequest; }
    public void setJobRequest(JobRequest jobRequest) { this.jobRequest = jobRequest; }

    public User getFreelancer() { return freelancer; }
    public void setFreelancer(User freelancer) { this.freelancer = freelancer; }

    public Double getRate() { return rate; }
    public void setRate(Double rate) { this.rate = rate; }

    public String getProposal() { return proposal; }
    public void setProposal(String proposal) { this.proposal = proposal; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
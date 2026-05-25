# SkillBridge SL — Freelance Bidding Platform

> **Developer:** Manusha Ranaweera &nbsp;|&nbsp; **Stack:** React 18 + Spring Boot 3 + PostgreSQL &nbsp;|&nbsp; **Year:** 2025

Sri Lanka's trusted freelance bidding marketplace. Clients post jobs. Freelancers submit bids. Funds are held in **escrow** and only released when the client approves the work — solving the #1 problem with Facebook group hiring: no payment protection.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [System Architecture](#2-system-architecture)
3. [Folder Structure](#3-folder-structure)
4. [Database Schema](#4-database-schema)
5. [Entities — Java Code](#5-entities--java-code)
6. [Enums](#6-enums)
7. [Repositories](#7-repositories)
8. [Service Layer](#8-service-layer)
9. [Security — JWT Setup](#9-security--jwt-setup)
10. [REST API Reference](#10-rest-api-reference)
11. [State Machines](#11-state-machines)
12. [DTOs](#12-dtos)
13. [Exception Handling](#13-exception-handling)
14. [React Frontend](#14-react-frontend)
15. [Docker & Docker Compose](#15-docker--docker-compose)
16. [Nginx Configuration](#16-nginx-configuration)
17. [GitHub Actions CI/CD](#17-github-actions-cicd)
18. [Flyway Migrations](#18-flyway-migrations)
19. [application.yml](#19-applicationyml)
20. [Build Order — What to Code Next](#20-build-order--what-to-code-next)
21. [Interview Talking Points](#21-interview-talking-points)

---

## 1. Project Overview

| Item | Detail |
|---|---|
| Platform type | Two-sided freelance bidding marketplace |
| Market | Sri Lanka — all amounts in LKR |
| How it works | Client posts job → Freelancers bid → Client accepts best bid → Escrow holds funds → Freelancer delivers → Client approves → Escrow released |
| Revenue model | 10% platform commission on every completed order |
| Differentiator | Escrow-protected payments + verified freelancer profiles + dispute resolution |
| Deployment | DigitalOcean Droplet, Docker, Nginx, HTTPS |

---

## 2. System Architecture

```
Browser (HTTPS :443)
        │
        ▼
  ┌──────────┐
  │  Nginx   │  ← Reverse proxy, SSL termination, gzip, security headers
  └────┬─────┘
       │
       ├── /api/**  ──────────────────────────────────► Spring Boot API :8080
       │                                                        │
       │                                                        ▼
       │                                               PostgreSQL :5432
       │
       └── /* (all other) ──────────────────────────► React build (SPA)
                                                       served by Nginx

External services:
  Spring Boot API ──► Cloudinary CDN  (profile images, portfolio uploads)

All containers run via Docker Compose on a DigitalOcean Droplet.
```

### Request Flow

1. Browser sends `HTTPS` request to `skillbridgesl.lk`
2. Nginx terminates TLS (Let's Encrypt cert)
3. Path `/api/**` → proxied to Spring Boot on port 8080
4. All other paths → serve `index.html` (React SPA fallback)
5. Spring Boot validates JWT from `Authorization: Bearer <token>` header
6. Service layer executes business logic, queries PostgreSQL via JPA
7. JSON response returned through Nginx to browser

---

## 3. Folder Structure

```
skillbridge/
│
├── skillbridge-api/                        ← Spring Boot backend
│   ├── Dockerfile
│   └── src/main/
│       ├── java/com/skillbridge/
│       │   ├── config/
│       │   │   ├── SecurityConfig.java     ← Spring Security 6 filter chain
│       │   │   ├── JwtConfig.java          ← JWT secret + expiry from env
│       │   │   └── CorsConfig.java         ← Allow React dev server
│       │   │
│       │   ├── controller/
│       │   │   ├── AuthController.java     ← /api/v1/auth/**
│       │   │   ├── JobRequestController.java ← /api/v1/jobs/**
│       │   │   ├── BidController.java      ← /api/v1/jobs/{id}/bids/**
│       │   │   ├── OrderController.java    ← lifecycle transitions
│       │   │   ├── ReviewController.java   ← /api/v1/reviews/**
│       │   │   ├── MessageController.java  ← /api/v1/messages/**
│       │   │   └── AdminController.java    ← /api/v1/admin/**
│       │   │
│       │   ├── service/
│       │   │   ├── UserService.java
│       │   │   ├── JobRequestService.java
│       │   │   ├── BidService.java
│       │   │   ├── EscrowService.java      ← @Transactional — most critical
│       │   │   ├── OrderLifecycleService.java ← state machine enforcement
│       │   │   ├── ReviewService.java
│       │   │   └── MessageService.java
│       │   │
│       │   ├── repository/
│       │   │   ├── UserRepository.java
│       │   │   ├── FreelancerProfileRepository.java
│       │   │   ├── JobRequestRepository.java
│       │   │   ├── BidRepository.java      ← ✅ EXISTS
│       │   │   ├── EscrowTransactionRepository.java
│       │   │   ├── ReviewRepository.java
│       │   │   └── MessageRepository.java
│       │   │
│       │   ├── entity/
│       │   │   ├── User.java               ← ✅ EXISTS — needs update
│       │   │   ├── FreelancerProfile.java  ← NEW
│       │   │   ├── JobRequest.java         ← ✅ EXISTS — needs update
│       │   │   ├── Bid.java                ← ✅ EXISTS — needs update
│       │   │   ├── EscrowTransaction.java  ← NEW
│       │   │   ├── Review.java             ← NEW
│       │   │   └── Message.java            ← NEW
│       │   │
│       │   ├── enums/
│       │   │   ├── Role.java               ← CLIENT, FREELANCER, ADMIN
│       │   │   ├── JobStatus.java          ← full state machine
│       │   │   ├── BidStatus.java          ← PENDING, ACCEPTED, REJECTED
│       │   │   └── EscrowStatus.java       ← HELD, RELEASED, REFUNDED, FROZEN
│       │   │
│       │   ├── dto/
│       │   │   ├── request/
│       │   │   │   ├── RegisterRequest.java
│       │   │   │   ├── LoginRequest.java
│       │   │   │   ├── JobRequestDTO.java
│       │   │   │   ├── BidDTO.java
│       │   │   │   └── ReviewDTO.java
│       │   │   └── response/
│       │   │       ├── JwtResponse.java
│       │   │       ├── UserResponse.java
│       │   │       ├── JobResponse.java
│       │   │       ├── BidResponse.java
│       │   │       └── ErrorResponse.java
│       │   │
│       │   ├── exception/
│       │   │   ├── GlobalExceptionHandler.java
│       │   │   ├── ResourceNotFoundException.java
│       │   │   ├── UnauthorizedException.java
│       │   │   └── InvalidStateException.java
│       │   │
│       │   └── security/
│       │       ├── JwtUtil.java
│       │       ├── JwtAuthFilter.java
│       │       └── UserDetailsServiceImpl.java
│       │
│       └── resources/
│           ├── db/migration/
│           │   ├── V1__init_users.sql
│           │   ├── V2__freelancer_profiles.sql
│           │   ├── V3__job_requests.sql
│           │   ├── V4__bids.sql
│           │   ├── V5__escrow_transactions.sql
│           │   └── V6__reviews_messages.sql
│           └── application.yml
│
├── skillbridge-ui/                         ← React frontend
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── api/
│       │   ├── axiosInstance.js 
│       │   ├── authApi.js
│       │   ├── jobsApi.js
│       │   ├── bidsApi.js
│       │   ├── orderApi.js
│       │   └── reviewsApi.js
│       ├── context/
│       │   └── AuthContext.jsx
│       ├── components/
│       │   ├── Navbar.jsx
│       │   ├── JobCard.jsx
│       │   ├── BidCard.jsx
│       │   ├── ReviewCard.jsx
│       │   ├── ProtectedRoute.jsx
│       │   ├── StatusBadge.jsx
│       │   └── EscrowBanner.jsx
│       ├── pages/
│       │   ├── HomePage.jsx
│       │   ├── JobDetailPage.jsx
│       │   ├── PostJobPage.jsx
│       │   ├── ClientDashboard.jsx
│       │   ├── FreelancerDashboard.jsx
│       │   ├── ProfilePage.jsx
│       │   ├── RegisterPage.jsx
│       │   ├── LoginPage.jsx
│       │   └── AdminPage.jsx
│       └── hooks/
│           ├── useAuth.js
│           ├── useJobs.js
│           └── useBids.js
│
└── infrastructure/
    ├── docker-compose.yml
    ├── nginx/
    │   └── nginx.conf
    └── .github/
        └── workflows/
            └── deploy.yml
```

---

## 4. Database Schema

### `users`
```sql
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) UNIQUE NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    username            VARCHAR(100) NOT NULL,
    role                VARCHAR(20)  NOT NULL,          -- CLIENT | FREELANCER | ADMIN
    is_verified         BOOLEAN DEFAULT false,
    is_active           BOOLEAN DEFAULT true,
    wallet_balance_lkr  DECIMAL(12,2) DEFAULT 0,
    created_at          TIMESTAMPTZ DEFAULT now()
);
```

### `freelancer_profiles`
```sql
CREATE TABLE freelancer_profiles (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT UNIQUE REFERENCES users(id),
    bio                 TEXT,
    city                VARCHAR(100),
    hourly_rate_lkr     DECIMAL(10,2),
    avg_rating          DECIMAL(3,2) DEFAULT 0.00,
    total_reviews       INTEGER DEFAULT 0,
    profile_image_url   VARCHAR(500),
    portfolio_url       VARCHAR(500)
);
```

### `job_requests`
```sql
CREATE TABLE job_requests (
    id              BIGSERIAL PRIMARY KEY,
    client_id       BIGINT REFERENCES users(id),
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    category        VARCHAR(50)  NOT NULL,
    budget_lkr      DECIMAL(10,2) NOT NULL,
    deadline        DATE,
    status          VARCHAR(30) NOT NULL,
    accepted_bid_id BIGINT,
    created_at      TIMESTAMPTZ DEFAULT now()
);
```

### `bids`
```sql
CREATE TABLE bids (
    id                      BIGSERIAL PRIMARY KEY,
    job_request_id          BIGINT REFERENCES job_requests(id),
    freelancer_id           BIGINT REFERENCES users(id),
    rate_lkr                DECIMAL(10,2) NOT NULL,
    proposal                TEXT NOT NULL,
    proposed_duration_days  INTEGER,
    status                  VARCHAR(20) DEFAULT 'PENDING',
    created_at              TIMESTAMPTZ DEFAULT now()
);
```

### `escrow_transactions`
```sql
CREATE TABLE escrow_transactions (
    id              BIGSERIAL PRIMARY KEY,
    job_request_id  BIGINT UNIQUE REFERENCES job_requests(id),
    amount_lkr      DECIMAL(12,2) NOT NULL,
    platform_fee_lkr DECIMAL(12,2) NOT NULL,
    status          VARCHAR(20) NOT NULL,               -- HELD | RELEASED | REFUNDED | FROZEN
    held_at         TIMESTAMPTZ DEFAULT now(),
    released_at     TIMESTAMPTZ
);
```

### `reviews`
```sql
CREATE TABLE reviews (
    id              BIGSERIAL PRIMARY KEY,
    job_request_id  BIGINT UNIQUE REFERENCES job_requests(id),
    reviewer_id     BIGINT REFERENCES users(id),
    freelancer_id   BIGINT REFERENCES users(id),
    rating          INTEGER CHECK (rating BETWEEN 1 AND 5) NOT NULL,
    comment         TEXT,
    created_at      TIMESTAMPTZ DEFAULT now()
);
```

### `messages`
```sql
CREATE TABLE messages (
    id              BIGSERIAL PRIMARY KEY,
    job_request_id  BIGINT REFERENCES job_requests(id),
    sender_id       BIGINT REFERENCES users(id),
    content         TEXT NOT NULL,
    sent_at         TIMESTAMPTZ DEFAULT now()
);
```

---

## 5. Entities — Java Code

### `User.java` ✅ EXISTS — update to match this

```java
package com.skillbridge.entity;

import com.skillbridge.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private boolean isVerified = false;
    private boolean isActive = true;

    @Column(precision = 12, scale = 2)
    private BigDecimal walletBalanceLkr = BigDecimal.ZERO;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }
}
```

### `FreelancerProfile.java` 🆕 NEW

```java
package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "freelancer_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FreelancerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String city;

    @Column(precision = 10, scale = 2)
    private BigDecimal hourlyRateLkr;

    private Double avgRating = 0.0;
    private Integer totalReviews = 0;
    private String profileImageUrl;
    private String portfolioUrl;
}
```

### `JobRequest.java` ✅ EXISTS — update to match this

```java
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

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal budgetLkr;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status;

    private Long acceptedBidId;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = JobStatus.OPEN;
    }
}
```

### `Bid.java` ✅ EXISTS — update to match this

```java
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

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal rateLkr;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String proposal;

    private Integer proposedDurationDays;

    @Enumerated(EnumType.STRING)
    private BidStatus status = BidStatus.PENDING;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }
}
```

### `EscrowTransaction.java` 🆕 NEW

```java
package com.skillbridge.entity;

import com.skillbridge.enums.EscrowStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "escrow_transactions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EscrowTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "job_request_id", unique = true)
    private JobRequest jobRequest;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal amountLkr;

    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal platformFeeLkr;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscrowStatus status;

    private LocalDateTime heldAt;
    private LocalDateTime releasedAt;

    @PrePersist
    void onCreate() { this.heldAt = LocalDateTime.now(); }
}
```

### `Review.java` 🆕 NEW

```java
package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "job_request_id", unique = true)
    private JobRequest jobRequest;

    @ManyToOne
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @ManyToOne
    @JoinColumn(name = "freelancer_id")
    private User freelancer;

    @Column(nullable = false)
    private Integer rating;     // 1 – 5

    private String comment;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { this.createdAt = LocalDateTime.now(); }
}
```

### `Message.java` 🆕 NEW

```java
package com.skillbridge.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_request_id")
    private JobRequest jobRequest;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private LocalDateTime sentAt;

    @PrePersist
    void onCreate() { this.sentAt = LocalDateTime.now(); }
}
```

---

## 6. Enums

```java
// Role.java
package com.skillbridge.enums;
public enum Role { CLIENT, FREELANCER, ADMIN }

// JobStatus.java
package com.skillbridge.enums;
public enum JobStatus {
    OPEN, CLOSED, IN_PROGRESS, DELIVERED,
    REVISION_REQUESTED, COMPLETED, DISPUTED, CANCELLED
}

// BidStatus.java
package com.skillbridge.enums;
public enum BidStatus { PENDING, ACCEPTED, REJECTED }

// EscrowStatus.java
package com.skillbridge.enums;
public enum EscrowStatus { HELD, RELEASED, REFUNDED, FROZEN }
```

---

## 7. Repositories

```java
// UserRepository.java
package com.skillbridge.repository;
import com.skillbridge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}

// FreelancerProfileRepository.java
package com.skillbridge.repository;
import com.skillbridge.entity.FreelancerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface FreelancerProfileRepository extends JpaRepository<FreelancerProfile, Long> {
    Optional<FreelancerProfile> findByUserId(Long userId);
}

// JobRequestRepository.java
package com.skillbridge.repository;
import com.skillbridge.entity.JobRequest;
import com.skillbridge.enums.JobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface JobRequestRepository extends JpaRepository<JobRequest, Long> {
    List<JobRequest> findByClientId(Long clientId);
    List<JobRequest> findByStatus(JobStatus status);
    List<JobRequest> findByStatusAndCategory(JobStatus status, String category);
}

// BidRepository.java  ✅ EXISTS — add the new method
package com.skillbridge.repository;
import com.skillbridge.entity.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByJobRequestId(Long jobRequestId);
    List<Bid> findByFreelancerId(Long freelancerId);
    boolean existsByJobRequestIdAndFreelancerId(Long jobRequestId, Long freelancerId); // NEW
}

// EscrowTransactionRepository.java
package com.skillbridge.repository;
import com.skillbridge.entity.EscrowTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, Long> {
    Optional<EscrowTransaction> findByJobRequestId(Long jobRequestId);
}

// ReviewRepository.java
package com.skillbridge.repository;
import com.skillbridge.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);
    boolean existsByJobRequestId(Long jobRequestId);
}

// MessageRepository.java
package com.skillbridge.repository;
import com.skillbridge.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByJobRequestIdOrderBySentAtAsc(Long jobRequestId);
}
```

---

## 8. Service Layer

### `UserService.java`

```java
package com.skillbridge.service;

import com.skillbridge.dto.request.RegisterRequest;
import com.skillbridge.dto.response.UserResponse;
import com.skillbridge.entity.FreelancerProfile;
import com.skillbridge.entity.User;
import com.skillbridge.enums.Role;
import com.skillbridge.exception.ResourceNotFoundException;
import com.skillbridge.repository.FreelancerProfileRepository;
import com.skillbridge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final FreelancerProfileRepository freelancerProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public UserResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail()))
            throw new IllegalArgumentException("Email already registered");

        User user = new User();
        user.setEmail(req.getEmail());
        user.setUsername(req.getUsername());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole());
        userRepository.save(user);

        // Auto-create freelancer profile if role is FREELANCER
        if (user.getRole() == Role.FREELANCER) {
            FreelancerProfile profile = new FreelancerProfile();
            profile.setUser(user);
            freelancerProfileRepository.save(profile);
        }

        return toResponse(user);
    }

    public User loadByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getUsername(), u.getRole().name());
    }
}
```

### `JobRequestService.java`

```java
package com.skillbridge.service;

import com.skillbridge.dto.request.JobRequestDTO;
import com.skillbridge.dto.response.JobResponse;
import com.skillbridge.entity.*;
import com.skillbridge.enums.*;
import com.skillbridge.exception.*;
import com.skillbridge.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class JobRequestService {

    private final JobRequestRepository jobRequestRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final EscrowService escrowService;

    public JobResponse postJob(Long clientId, JobRequestDTO dto) {
        User client = userRepository.findById(clientId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (client.getRole() != Role.CLIENT)
            throw new UnauthorizedException("Only clients can post jobs");

        JobRequest job = new JobRequest();
        job.setClient(client);
        job.setTitle(dto.getTitle());
        job.setDescription(dto.getDescription());
        job.setCategory(dto.getCategory());
        job.setBudgetLkr(dto.getBudgetLkr());
        job.setDeadline(dto.getDeadline());
        return toResponse(jobRequestRepository.save(job));
    }

    public List<JobResponse> listOpenJobs(String category) {
        List<JobRequest> jobs = (category != null)
            ? jobRequestRepository.findByStatusAndCategory(JobStatus.OPEN, category)
            : jobRequestRepository.findByStatus(JobStatus.OPEN);
        return jobs.stream().map(this::toResponse).toList();
    }

    @Transactional
    public void acceptBid(Long clientId, Long jobId, Long bidId) {
        JobRequest job = findJobOrThrow(jobId);
        if (!job.getClient().getId().equals(clientId))
            throw new UnauthorizedException("Not your job");
        if (job.getStatus() != JobStatus.OPEN)
            throw new InvalidStateException("Job is not OPEN");

        Bid accepted = bidRepository.findById(bidId)
            .orElseThrow(() -> new ResourceNotFoundException("Bid not found"));
        accepted.setStatus(BidStatus.ACCEPTED);
        bidRepository.save(accepted);

        // Reject all other bids
        bidRepository.findByJobRequestId(jobId).stream()
            .filter(b -> !b.getId().equals(bidId))
            .forEach(b -> { b.setStatus(BidStatus.REJECTED); bidRepository.save(b); });

        // Hold escrow
        escrowService.holdEscrow(job, accepted.getRateLkr());

        job.setAcceptedBidId(bidId);
        job.setStatus(JobStatus.CLOSED);
        jobRequestRepository.save(job);
    }

    public List<JobResponse> getMyJobs(Long clientId) {
        return jobRequestRepository.findByClientId(clientId)
            .stream().map(this::toResponse).toList();
    }

    JobRequest findJobOrThrow(Long id) {
        return jobRequestRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
    }

    private JobResponse toResponse(JobRequest j) {
        return new JobResponse(j.getId(), j.getTitle(), j.getDescription(),
            j.getCategory(), j.getBudgetLkr(), j.getStatus().name(),
            j.getClient().getUsername(), j.getCreatedAt());
    }
}
```

### `BidService.java`

```java
package com.skillbridge.service;

import com.skillbridge.dto.request.BidDTO;
import com.skillbridge.dto.response.BidResponse;
import com.skillbridge.entity.*;
import com.skillbridge.enums.*;
import com.skillbridge.exception.*;
import com.skillbridge.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final JobRequestRepository jobRequestRepository;
    private final UserRepository userRepository;

    public BidResponse submitBid(Long freelancerId, Long jobId, BidDTO dto) {
        User freelancer = userRepository.findById(freelancerId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (freelancer.getRole() != Role.FREELANCER)
            throw new UnauthorizedException("Only freelancers can bid");

        JobRequest job = jobRequestRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (job.getStatus() != JobStatus.OPEN)
            throw new InvalidStateException("Job is not accepting bids");
        if (bidRepository.existsByJobRequestIdAndFreelancerId(jobId, freelancerId))
            throw new IllegalArgumentException("You have already bid on this job");

        Bid bid = new Bid();
        bid.setJobRequest(job);
        bid.setFreelancer(freelancer);
        bid.setRateLkr(dto.getRateLkr());
        bid.setProposal(dto.getProposal());
        bid.setProposedDurationDays(dto.getProposedDurationDays());
        return toResponse(bidRepository.save(bid));
    }

    public List<BidResponse> getJobBids(Long jobId, Long requestingUserId) {
        JobRequest job = jobRequestRepository.findById(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        if (!job.getClient().getId().equals(requestingUserId))
            throw new UnauthorizedException("Only the client can see bids");
        return bidRepository.findByJobRequestId(jobId)
            .stream().map(this::toResponse).toList();
    }

    public List<BidResponse> getMyBids(Long freelancerId) {
        return bidRepository.findByFreelancerId(freelancerId)
            .stream().map(this::toResponse).toList();
    }

    private BidResponse toResponse(Bid b) {
        return new BidResponse(b.getId(), b.getRateLkr(), b.getProposal(),
            b.getProposedDurationDays(), b.getStatus().name(),
            b.getFreelancer().getUsername(), b.getCreatedAt());
    }
}
```

### `EscrowService.java` — Most Critical

```java
package com.skillbridge.service;

import com.skillbridge.entity.*;
import com.skillbridge.enums.EscrowStatus;
import com.skillbridge.exception.*;
import com.skillbridge.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EscrowService {

    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.10"); // 10%

    private final EscrowTransactionRepository escrowRepository;
    private final UserRepository userRepository;

    /** Called when client accepts a bid — holds the agreed amount */
    public EscrowTransaction holdEscrow(JobRequest job, BigDecimal amount) {
        BigDecimal fee = amount.multiply(PLATFORM_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
        EscrowTransaction escrow = new EscrowTransaction();
        escrow.setJobRequest(job);
        escrow.setAmountLkr(amount);
        escrow.setPlatformFeeLkr(fee);
        escrow.setStatus(EscrowStatus.HELD);
        return escrowRepository.save(escrow);
    }

    /** Called when client approves delivery — releases funds to freelancer */
    @Transactional
    public void releaseEscrow(Long jobId, User freelancer) {
        EscrowTransaction escrow = findEscrowOrThrow(jobId);
        if (escrow.getStatus() != EscrowStatus.HELD)
            throw new InvalidStateException("Escrow is not in HELD state");

        BigDecimal payout = escrow.getAmountLkr().subtract(escrow.getPlatformFeeLkr());
        freelancer.setWalletBalanceLkr(freelancer.getWalletBalanceLkr().add(payout));
        userRepository.save(freelancer);

        escrow.setStatus(EscrowStatus.RELEASED);
        escrow.setReleasedAt(LocalDateTime.now());
        escrowRepository.save(escrow);
    }

    /** Called when job is cancelled — refunds client in full */
    @Transactional
    public void refundEscrow(Long jobId, User client) {
        EscrowTransaction escrow = findEscrowOrThrow(jobId);
        client.setWalletBalanceLkr(client.getWalletBalanceLkr().add(escrow.getAmountLkr()));
        userRepository.save(client);
        escrow.setStatus(EscrowStatus.REFUNDED);
        escrow.setReleasedAt(LocalDateTime.now());
        escrowRepository.save(escrow);
    }

    /** Freezes escrow when a dispute is raised */
    public void freezeEscrow(Long jobId) {
        EscrowTransaction escrow = findEscrowOrThrow(jobId);
        escrow.setStatus(EscrowStatus.FROZEN);
        escrowRepository.save(escrow);
    }

    /** Admin resolves dispute — split between freelancer and client */
    @Transactional
    public void resolveDispute(Long jobId, User client, User freelancer,
                                BigDecimal freelancerPercent) {
        EscrowTransaction escrow = findEscrowOrThrow(jobId);
        BigDecimal total = escrow.getAmountLkr();
        BigDecimal toFreelancer = total.multiply(freelancerPercent)
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal toClient = total.subtract(toFreelancer);

        freelancer.setWalletBalanceLkr(freelancer.getWalletBalanceLkr().add(toFreelancer));
        client.setWalletBalanceLkr(client.getWalletBalanceLkr().add(toClient));
        userRepository.saveAll(java.util.List.of(freelancer, client));

        escrow.setStatus(EscrowStatus.RELEASED);
        escrow.setReleasedAt(LocalDateTime.now());
        escrowRepository.save(escrow);
    }

    private EscrowTransaction findEscrowOrThrow(Long jobId) {
        return escrowRepository.findByJobRequestId(jobId)
            .orElseThrow(() -> new ResourceNotFoundException("Escrow not found for job " + jobId));
    }
}
```

### `OrderLifecycleService.java` — State Machine

```java
package com.skillbridge.service;

import com.skillbridge.entity.*;
import com.skillbridge.enums.JobStatus;
import com.skillbridge.exception.InvalidStateException;
import com.skillbridge.exception.UnauthorizedException;
import com.skillbridge.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderLifecycleService {

    private final JobRequestRepository jobRequestRepository;
    private final BidRepository bidRepository;
    private final EscrowService escrowService;
    private final UserRepository userRepository;

    /** FREELANCER: CLOSED → IN_PROGRESS */
    @Transactional
    public void startWork(Long freelancerId, Long jobId) {
        JobRequest job = findAndValidateFreelancer(freelancerId, jobId);
        requireStatus(job, JobStatus.CLOSED);
        job.setStatus(JobStatus.IN_PROGRESS);
        jobRequestRepository.save(job);
    }

    /** FREELANCER: IN_PROGRESS → DELIVERED */
    @Transactional
    public void deliverWork(Long freelancerId, Long jobId) {
        JobRequest job = findAndValidateFreelancer(freelancerId, jobId);
        requireStatus(job, JobStatus.IN_PROGRESS);
        job.setStatus(JobStatus.DELIVERED);
        jobRequestRepository.save(job);
    }

    /** CLIENT: DELIVERED → COMPLETED — releases escrow */
    @Transactional
    public void approveDelivery(Long clientId, Long jobId) {
        JobRequest job = findAndValidateClient(clientId, jobId);
        requireStatus(job, JobStatus.DELIVERED);

        User freelancer = getFreelancerForJob(job);
        escrowService.releaseEscrow(jobId, freelancer);

        job.setStatus(JobStatus.COMPLETED);
        jobRequestRepository.save(job);
    }

    /** CLIENT: DELIVERED → REVISION_REQUESTED */
    @Transactional
    public void requestRevision(Long clientId, Long jobId) {
        JobRequest job = findAndValidateClient(clientId, jobId);
        requireStatus(job, JobStatus.DELIVERED);
        job.setStatus(JobStatus.REVISION_REQUESTED);
        jobRequestRepository.save(job);
    }

    /** CLIENT or FREELANCER: any active status → DISPUTED */
    @Transactional
    public void raiseDispute(Long userId, Long jobId) {
        JobRequest job = jobRequestRepository.findById(jobId)
            .orElseThrow(() -> new InvalidStateException("Job not found"));
        escrowService.freezeEscrow(jobId);
        job.setStatus(JobStatus.DISPUTED);
        jobRequestRepository.save(job);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private void requireStatus(JobRequest job, JobStatus required) {
        if (job.getStatus() != required)
            throw new InvalidStateException(
                "Expected status " + required + " but was " + job.getStatus());
    }

    private JobRequest findAndValidateClient(Long clientId, Long jobId) {
        JobRequest job = jobRequestRepository.findById(jobId)
            .orElseThrow(() -> new InvalidStateException("Job not found"));
        if (!job.getClient().getId().equals(clientId))
            throw new UnauthorizedException("Not your job");
        return job;
    }

    private JobRequest findAndValidateFreelancer(Long freelancerId, Long jobId) {
        JobRequest job = jobRequestRepository.findById(jobId)
            .orElseThrow(() -> new InvalidStateException("Job not found"));
        User freelancer = getFreelancerForJob(job);
        if (!freelancer.getId().equals(freelancerId))
            throw new UnauthorizedException("You are not assigned to this job");
        return job;
    }

    private User getFreelancerForJob(JobRequest job) {
        if (job.getAcceptedBidId() == null)
            throw new InvalidStateException("No accepted bid on this job");
        return bidRepository.findById(job.getAcceptedBidId())
            .orElseThrow(() -> new InvalidStateException("Accepted bid not found"))
            .getFreelancer();
    }
}
```

---

## 9. Security — JWT Setup

### `JwtUtil.java`

```java
package com.skillbridge.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiry-ms}")
    private long expiryMs;

    private Key key() { return Keys.hmacShaKeyFor(secret.getBytes()); }

    public String generateToken(String email, String role) {
        return Jwts.builder()
            .setSubject(email)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + expiryMs))
            .signWith(key(), SignatureAlgorithm.HS256)
            .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build()
            .parseClaimsJws(token).getBody().getSubject();
    }

    public boolean isValid(String token) {
        try { Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }
}
```

### `JwtAuthFilter.java`

```java
package com.skillbridge.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtUtil.isValid(token)) {
                String email = jwtUtil.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
```

### `SecurityConfig.java`

```java
package com.skillbridge.config;

import com.skillbridge.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/jobs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
}
```

---

## 10. REST API Reference

### Auth
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Public | `{ email, username, password, role }` |
| POST | `/api/v1/auth/login` | Public | `{ email, password }` → `{ accessToken }` |

### Jobs
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/jobs` | Public | List OPEN jobs. `?category=&maxBudget=` |
| GET | `/api/v1/jobs/{id}` | Public | Job detail |
| POST | `/api/v1/jobs` | CLIENT | Post new job |
| PUT | `/api/v1/jobs/{id}` | CLIENT (owner) | Edit job (only if OPEN) |
| GET | `/api/v1/jobs/my` | CLIENT | My posted jobs |

### Bids
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/jobs/{id}/bids` | CLIENT (owner) | View bids on job |
| POST | `/api/v1/jobs/{id}/bids` | FREELANCER | Submit bid |
| DELETE | `/api/v1/bids/{id}` | FREELANCER (owner) | Withdraw bid |
| GET | `/api/v1/bids/my` | FREELANCER | My submitted bids |

### Order Lifecycle
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/jobs/{id}/accept-bid/{bidId}` | CLIENT | Accept bid → escrow held |
| PATCH | `/api/v1/jobs/{id}/start` | FREELANCER | Start work |
| PATCH | `/api/v1/jobs/{id}/deliver` | FREELANCER | Deliver work |
| PATCH | `/api/v1/jobs/{id}/approve` | CLIENT | Approve → escrow released |
| PATCH | `/api/v1/jobs/{id}/revision` | CLIENT | Request revision |
| POST | `/api/v1/jobs/{id}/dispute` | CLIENT or FREELANCER | Raise dispute |

### Reviews & Messages
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/reviews` | CLIENT | Submit review (job must be COMPLETED) |
| GET | `/api/v1/reviews/freelancer/{id}` | Public | Freelancer reviews |
| GET | `/api/v1/messages/{jobId}` | Job parties | Message thread |
| POST | `/api/v1/messages/{jobId}` | Job parties | Send message |

### Admin
| Method | Endpoint | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/admin/disputes` | ADMIN | All disputed jobs |
| POST | `/api/v1/admin/disputes/{jobId}/resolve` | ADMIN | Resolve dispute |
| POST | `/api/v1/admin/users/{id}/verify` | ADMIN | Verify freelancer |

---

## 11. State Machines

### Job Status Lifecycle

```
[Client posts job]
       │
       ▼
  ┌─────────┐     Client cancels (no bid accepted)
  │  OPEN   │ ──────────────────────────────────────► CANCELLED
  └─────────┘
       │
       │ Client accepts a bid → escrow HELD
       ▼
  ┌──────────┐
  │  CLOSED  │  (no more bids accepted)
  └──────────┘
       │
       │ Freelancer starts work
       ▼
  ┌─────────────┐
  │ IN_PROGRESS │ ◄────────────────────────────────────────────────┐
  └─────────────┘                                                   │
       │                                                            │
       │ Freelancer delivers                                        │
       ▼                                                            │
  ┌───────────┐    Client requests revision                         │
  │ DELIVERED │ ─────────────────────────► REVISION_REQUESTED ─────┘
  └───────────┘
       │
       │ Client approves → escrow RELEASED to freelancer
       ▼
  ┌───────────┐
  │ COMPLETED │  (review can now be submitted)
  └───────────┘

  Any active status ──► DISPUTED (escrow FROZEN) ──► Admin resolves
```

### Escrow Lifecycle

```
  Client accepts bid
         │
         ▼
    ┌────────┐
    │  HELD  │ ──── job cancelled ──────────────────► REFUNDED
    └────────┘        (client gets full amount back)
         │
         │ Dispute raised
         ▼
    ┌────────┐
    │ FROZEN │ ──── admin resolves ─────────────────► RELEASED or REFUNDED
    └────────┘
         │
         │ (no dispute — client approves directly from HELD)
         │
    Client approves delivery
         │
         ▼
    ┌──────────┐
    │ RELEASED │   90% → freelancer wallet  |  10% → platform fee
    └──────────┘
```

### Bid Lifecycle

```
  Freelancer submits bid
         │
         ▼
    ┌─────────┐
    │ PENDING │
    └─────────┘
         │
         ├── Client accepts THIS bid ──────────► ACCEPTED
         │
         ├── Client accepts another bid ────────► REJECTED
         │
         └── Freelancer withdraws ──────────────► (deleted)
```

---

## 12. DTOs

### Request DTOs

```java
// RegisterRequest.java
@Data
public class RegisterRequest {
    @NotBlank private String email;
    @NotBlank private String username;
    @NotBlank @Size(min = 8) private String password;
    @NotNull private Role role;
}

// LoginRequest.java
@Data
public class LoginRequest {
    @NotBlank private String email;
    @NotBlank private String password;
}

// JobRequestDTO.java
@Data
public class JobRequestDTO {
    @NotBlank private String title;
    @NotBlank private String description;
    @NotBlank private String category;
    @NotNull @Positive private BigDecimal budgetLkr;
    private LocalDate deadline;
}

// BidDTO.java
@Data
public class BidDTO {
    @NotNull @Positive private BigDecimal rateLkr;
    @NotBlank private String proposal;
    @Positive private Integer proposedDurationDays;
}

// ReviewDTO.java
@Data
public class ReviewDTO {
    @NotNull @Min(1) @Max(5) private Integer rating;
    private String comment;
}
```

### Response DTOs

```java
// JwtResponse.java
@Data @AllArgsConstructor
public class JwtResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private String role;
}

// UserResponse.java
@Data @AllArgsConstructor
public class UserResponse {
    private Long id;
    private String email;
    private String username;
    private String role;
}

// JobResponse.java
@Data @AllArgsConstructor
public class JobResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private BigDecimal budgetLkr;
    private String status;
    private String clientUsername;
    private LocalDateTime createdAt;
}

// BidResponse.java
@Data @AllArgsConstructor
public class BidResponse {
    private Long id;
    private BigDecimal rateLkr;
    private String proposal;
    private Integer proposedDurationDays;
    private String status;
    private String freelancerUsername;
    private LocalDateTime createdAt;
}

// ErrorResponse.java
@Data @AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
}
```

---

## 13. Exception Handling

```java
// ResourceNotFoundException.java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String msg) { super(msg); }
}

// UnauthorizedException.java
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String msg) { super(msg); }
}

// InvalidStateException.java
public class InvalidStateException extends RuntimeException {
    public InvalidStateException(String msg) { super(msg); }
}

// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(404).body(error(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> unauthorized(UnauthorizedException ex) {
        return ResponseEntity.status(403).body(error(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ErrorResponse> invalidState(InvalidStateException ex) {
        return ResponseEntity.status(422).body(error(422, "Invalid State", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(400).body(error(400, "Bad Request", ex.getMessage()));
    }

    private ErrorResponse error(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}
```

---

## 14. React Frontend

### `axiosInstance.js`

```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
});

// Attach JWT to every request
api.interceptors.request.use(config => {
  const token = localStorage.getItem('token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export default api;
```

### `AuthContext.jsx`

```jsx
import { createContext, useContext, useState } from 'react';
import api from '../api/axiosInstance';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user');
    return stored ? JSON.parse(stored) : null;
  });

  const login = async (email, password) => {
    const res = await api.post('/api/v1/auth/login', { email, password });
    localStorage.setItem('token', res.data.accessToken);
    localStorage.setItem('user', JSON.stringify(res.data));
    setUser(res.data);
  };

  const logout = () => {
    localStorage.clear();
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
```

### `ProtectedRoute.jsx`

```jsx
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

export default function ProtectedRoute({ children, allowedRoles }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" />;
  if (allowedRoles && !allowedRoles.includes(user.role))
    return <Navigate to="/" />;
  return children;
}
```

### `StatusBadge.jsx`

```jsx
const colors = {
  OPEN:               'bg-green-100 text-green-800',
  CLOSED:             'bg-gray-100 text-gray-700',
  IN_PROGRESS:        'bg-blue-100 text-blue-800',
  DELIVERED:          'bg-purple-100 text-purple-800',
  REVISION_REQUESTED: 'bg-yellow-100 text-yellow-800',
  COMPLETED:          'bg-teal-100 text-teal-800',
  DISPUTED:           'bg-red-100 text-red-800',
  CANCELLED:          'bg-red-50 text-red-500',
};

export default function StatusBadge({ status }) {
  return (
    <span className={`text-xs font-medium px-2.5 py-0.5 rounded-full ${colors[status] || 'bg-gray-100 text-gray-600'}`}>
      {status}
    </span>
  );
}
```

### `App.jsx` — Routes

```jsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Navbar from './components/Navbar';
import HomePage from './pages/HomePage';
import JobDetailPage from './pages/JobDetailPage';
import PostJobPage from './pages/PostJobPage';
import ClientDashboard from './pages/ClientDashboard';
import FreelancerDashboard from './pages/FreelancerDashboard';
import ProfilePage from './pages/ProfilePage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Navbar />
        <Routes>
          <Route path="/"              element={<HomePage />} />
          <Route path="/jobs/:id"      element={<JobDetailPage />} />
          <Route path="/login"         element={<LoginPage />} />
          <Route path="/register"      element={<RegisterPage />} />
          <Route path="/freelancers/:id" element={<ProfilePage />} />
          <Route path="/jobs/new" element={
            <ProtectedRoute allowedRoles={['CLIENT']}>
              <PostJobPage />
            </ProtectedRoute>
          }/>
          <Route path="/dashboard/client" element={
            <ProtectedRoute allowedRoles={['CLIENT']}>
              <ClientDashboard />
            </ProtectedRoute>
          }/>
          <Route path="/dashboard/freelancer" element={
            <ProtectedRoute allowedRoles={['FREELANCER']}>
              <FreelancerDashboard />
            </ProtectedRoute>
          }/>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
```

---

## 15. Docker & Docker Compose

### `docker-compose.yml`

```yaml
version: '3.9'

services:
  postgres:
    image: postgres:16-alpine
    container_name: skillbridge-db
    environment:
      POSTGRES_DB: skillbridge
      POSTGRES_USER: ${DB_USERNAME}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
    networks:
      - skillbridge-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME}"]
      interval: 10s
      timeout: 5s
      retries: 5

  skillbridge-api:
    build: ./skillbridge-api
    container_name: skillbridge-api
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/skillbridge
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      JWT_EXPIRY_MS: 900000
      CLOUDINARY_URL: ${CLOUDINARY_URL}
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - skillbridge-net
    restart: unless-stopped

  skillbridge-ui:
    build: ./skillbridge-ui
    container_name: skillbridge-ui
    networks:
      - skillbridge-net
    restart: unless-stopped

  nginx:
    image: nginx:alpine
    container_name: skillbridge-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./infrastructure/nginx/nginx.conf:/etc/nginx/nginx.conf
      - /etc/letsencrypt:/etc/letsencrypt:ro
    depends_on:
      - skillbridge-api
      - skillbridge-ui
    networks:
      - skillbridge-net
    restart: unless-stopped

networks:
  skillbridge-net:
    driver: bridge

volumes:
  postgres_data:
```

### `skillbridge-api/Dockerfile`

```dockerfile
# Stage 1 — Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# Stage 2 — Run
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### `skillbridge-ui/Dockerfile`

```dockerfile
# Stage 1 — Build
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json .
RUN npm ci --silent
COPY . .
RUN npm run build

# Stage 2 — Serve
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### `skillbridge-ui/nginx.conf`

```nginx
server {
  listen 80;
  root /usr/share/nginx/html;
  index index.html;

  location / {
    try_files $uri $uri/ /index.html;  # SPA fallback for React Router
  }
}
```

---

## 16. Nginx Configuration

`infrastructure/nginx/nginx.conf`

```nginx
worker_processes auto;

events { worker_connections 1024; }

http {
  gzip on;
  gzip_types text/plain application/json text/css application/javascript;
  gzip_min_length 1000;

  server {
    listen 80;
    server_name skillbridgesl.lk www.skillbridgesl.lk;
    return 301 https://$host$request_uri;
  }

  server {
    listen 443 ssl;
    server_name skillbridgesl.lk www.skillbridgesl.lk;

    ssl_certificate     /etc/letsencrypt/live/skillbridgesl.lk/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/skillbridgesl.lk/privkey.pem;
    ssl_protocols       TLSv1.2 TLSv1.3;

    # Security headers
    add_header X-Frame-Options DENY;
    add_header X-Content-Type-Options nosniff;
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains";

    # Spring Boot API
    location /api/ {
      proxy_pass http://skillbridge-api:8080;
      proxy_set_header Host $host;
      proxy_set_header X-Real-IP $remote_addr;
      proxy_set_header Authorization $http_authorization;
      proxy_pass_header Authorization;
    }

    # React SPA
    location / {
      proxy_pass http://skillbridge-ui:80;
      proxy_set_header Host $host;
    }
  }
}
```

---

## 17. GitHub Actions CI/CD

`.github/workflows/deploy.yml`

```yaml
name: Build and Deploy

on:
  push:
    branches: [ main ]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run tests
        run: cd skillbridge-api && mvn test

      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Build and push API image
        run: |
          docker build -t ghcr.io/${{ github.repository }}/api:${{ github.sha }} ./skillbridge-api
          docker push ghcr.io/${{ github.repository }}/api:${{ github.sha }}

      - name: Build and push UI image
        run: |
          docker build -t ghcr.io/${{ github.repository }}/ui:${{ github.sha }} ./skillbridge-ui
          docker push ghcr.io/${{ github.repository }}/ui:${{ github.sha }}

      - name: Deploy to DigitalOcean Droplet
        uses: appleboy/ssh-action@v1
        with:
          host: ${{ secrets.DROPLET_IP }}
          username: root
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            cd /opt/skillbridge
            docker compose pull
            docker compose up -d
            sleep 10
            curl -f http://localhost:8080/api/v1/actuator/health || exit 1
```

---

## 18. Flyway Migrations

`src/main/resources/db/migration/V1__init_users.sql`

```sql
CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) UNIQUE NOT NULL,
    password_hash       VARCHAR(255) NOT NULL,
    username            VARCHAR(100) NOT NULL,
    role                VARCHAR(20)  NOT NULL,
    is_verified         BOOLEAN DEFAULT false,
    is_active           BOOLEAN DEFAULT true,
    wallet_balance_lkr  DECIMAL(12,2) DEFAULT 0,
    created_at          TIMESTAMPTZ DEFAULT now()
);
```

`V3__job_requests.sql`

```sql
CREATE TABLE job_requests (
    id              BIGSERIAL PRIMARY KEY,
    client_id       BIGINT REFERENCES users(id),
    title           VARCHAR(200) NOT NULL,
    description     TEXT NOT NULL,
    category        VARCHAR(50) NOT NULL,
    budget_lkr      DECIMAL(10,2) NOT NULL,
    deadline        DATE,
    status          VARCHAR(30) NOT NULL,
    accepted_bid_id BIGINT,
    created_at      TIMESTAMPTZ DEFAULT now()
);
```

`V5__escrow_transactions.sql`

```sql
CREATE TABLE escrow_transactions (
    id               BIGSERIAL PRIMARY KEY,
    job_request_id   BIGINT UNIQUE REFERENCES job_requests(id),
    amount_lkr       DECIMAL(12,2) NOT NULL,
    platform_fee_lkr DECIMAL(12,2) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    held_at          TIMESTAMPTZ DEFAULT now(),
    released_at      TIMESTAMPTZ
);
```

---

## 19. application.yml

```yaml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: org.postgresql.Driver

  jpa:
    hibernate:
      ddl-auto: validate        # Flyway manages schema — never use create-drop
    show-sql: false
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect

  flyway:
    enabled: true
    locations: classpath:db/migration

app:
  jwt:
    secret: ${JWT_SECRET}
    expiry-ms: ${JWT_EXPIRY_MS:900000}

server:
  port: 8080

management:
  endpoints:
    web:
      exposure:
        include: health
```

---

## 20. Build Order — What to Code Next

Follow this order exactly. Each step must compile and run before moving to the next.

| Step | What | Why First |
|---|---|---|
| **1** | Create `enums/` — Role, JobStatus, BidStatus, EscrowStatus | Everything imports these |
| **2** | Update `User.java`, `JobRequest.java`, `Bid.java` to match this README | Entities must be correct before repositories compile |
| **3** | Add `FreelancerProfile.java`, `EscrowTransaction.java`, `Review.java`, `Message.java` | Complete the data model |
| **4** | Create all repositories (7 interfaces — one file each) | Services need them to compile |
| **5** | Add DTOs — `RegisterRequest`, `LoginRequest`, `JobRequestDTO`, `BidDTO`, responses | Services need these to compile |
| **6** | Add exception classes + `GlobalExceptionHandler` | Services throw these |
| **7** | `UserService` + `JwtUtil` + `JwtAuthFilter` + `SecurityConfig` | Auth must work before anything else |
| **8** | `AuthController` — test register + login in Postman | Verify JWT is working |
| **9** | `JobRequestService` + `JobRequestController` | Core feature |
| **10** | `BidService` + `BidController` | Freelancers can now bid |
| **11** | `EscrowService` + `OrderLifecycleService` + `OrderController` | The escrow state machine |
| **12** | `ReviewService` + `MessageService` + controllers | Polish |
| **13** | React frontend (pages + components) | After API is fully tested |
| **14** | Docker Compose local test → push to GitHub → CI/CD → DigitalOcean | Ship it |

---

## 21. Interview Talking Points

### Q: Why did you build this?
Facebook groups connect Sri Lankan freelancers and clients for free — but scams are common because there is no payment protection. I built the trust layer that Facebook cannot provide: escrow, verified profiles, and dispute resolution.

### Q: Explain your escrow state machine.
When a client accepts a bid, the agreed LKR amount is held in the `escrow_transactions` table with status `HELD`. The funds do not move until the client explicitly approves. The release is wrapped in `@Transactional` — it updates the escrow status and credits the freelancer wallet atomically. If either fails, the entire transaction rolls back. If a dispute is raised, the escrow becomes `FROZEN` — only an admin can resolve it.

### Q: How is the state machine enforced?
In `OrderLifecycleService`, every transition method calls `requireStatus()` first — it throws `InvalidStateException` if the job is not in the expected state. This means invalid transitions are impossible, not just unlikely. The enforcement is in the service layer, not the controller, so it applies regardless of how the service is called.

### Q: How does your security work?
Stateless JWT — access tokens expire in 15 minutes. Spring Security 6 protects endpoints by role via `requestMatchers`. Ownership validation (e.g. only the job's client can accept bids) is enforced in the service layer by comparing entity owner IDs with the authenticated user ID.

### Q: How would you scale this?
Move PostgreSQL to a managed database first (removes DB from the app server). Then run multiple Spring Boot instances behind Nginx upstream load balancing — stateless JWT means no sticky sessions needed. Messaging could upgrade from REST polling to WebSocket at higher load.

---

*SkillBridge SL — Manusha Ranaweera — 2025*
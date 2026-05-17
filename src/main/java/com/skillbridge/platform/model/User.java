package com.skillbridge.platform.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
// @Data // Lombok automatically generates getters, setters, toString, and equals methods
@AllArgsConstructor
@NoArgsConstructor // Generates a blank constructor required by Hibernate
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "user_role", nullable = false, length = 20)
    private String userRole; // Must hold either "CLIENT" or "FREELANCER"

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
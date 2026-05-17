
package com.skillbridge.platform.model;

public enum OrderStatus {
    PENDING,       // Order created, waiting for client payment deposit
    IN_PROGRESS,   // Client deposited money into escrow. Freelancer is safely working.
    DELIVERED,     // Freelancer submitted final project deliverables for review
    COMPLETED,     // Client approved work. Funds released from escrow to freelancer.
    DISPUTED       // Conflict flagged. Escrow funds locked until manual review.
}

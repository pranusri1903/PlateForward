package com.plateforward.user;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    public enum Role { DONOR, ORG, GIVER, TAKER, ADMIN }

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String email;
    private String passwordHash, name, phone, area;
    @Enumerated(EnumType.STRING)
    private Role role;
    private boolean verified = true; // only organizations start unverified
    private Instant createdAt = Instant.now();

    public boolean canGive() {
        return role == Role.DONOR || role == Role.GIVER;
    }
}

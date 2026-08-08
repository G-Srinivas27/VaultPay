package com.srinivas.vaultpay.user.entity;

import com.srinivas.vaultpay.common.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Entity representing a VaultPay user account.
 *
 * <p><b>Lombok annotations used:</b>
 * <ul>
 *   <li>{@code @Getter} — generates getters for all fields (read access for Spring/JPA)</li>
 *   <li>{@code @Setter} — generates setters (JPA needs them to hydrate objects from DB)</li>
 *   <li>{@code @NoArgsConstructor} — JPA REQUIRES a no-arg constructor to instantiate entities via reflection</li>
 *   <li>{@code @AllArgsConstructor} — needed by @Builder internally</li>
 *   <li>{@code @Builder} — enables the builder pattern: User.builder().email("x").build()</li>
 * </ul>
 *
 * <p><b>Why NOT use @Data?</b>
 * {@code @Data} generates equals/hashCode based on ALL fields. For JPA entities,
 * this is dangerous — it can cause infinite loops with bidirectional relationships
 * and incorrect Set/Map behavior. We control this explicitly instead.
 *
 * <p><b>Why is 'password' not in the DTO?</b>
 * The password field is NEVER returned to the client — only stored (hashed).
 * We'll explicitly exclude it from all response DTOs.
 *
 * <p><b>Audit fields:</b>
 * createdAt, updatedAt, createdBy, lastModifiedBy are inherited from
 * {@link BaseAuditEntity} and auto-populated by Spring Data JPA Auditing.
 * No @PrePersist or @PreUpdate needed here anymore.
 */
@Entity
@Table(
        name = "users",   // 'user' is a reserved keyword in SQL — always use 'users'
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phoneNumber;

    /**
     * Email serves as the unique business identifier (username) for login.
     * The uniqueness is enforced both at DB level (UniqueConstraint above)
     * and at application level (DuplicateResourceException in service).
     * Defence in depth — never rely on just one layer.
     */
    @Column(nullable = false, unique = true, length = 200)
    private String email;

    /**
     * ALWAYS stored as a BCrypt hash. NEVER the raw password.
     * The column is named 'password_hash' to make this contract explicit
     * to anyone reading the schema.
     */
    @Column(name = "password_hash", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)  // Store "USER"/"ADMIN" not 0/1 — safe if enum order changes
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;  // Every new user is a USER by default

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;  // Account is active upon creation
}

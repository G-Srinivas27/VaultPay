package com.srinivas.vaultpay.user.repository;

import com.srinivas.vaultpay.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Data access layer for the User entity.
 *
 * <p><b>Why extend JpaRepository and not CrudRepository?</b>
 * {@code JpaRepository<User, Long>} extends {@code CrudRepository} and adds:
 * <ul>
 *   <li>Batch operations (saveAll, deleteAll)</li>
 *   <li>Pagination and sorting support (findAll(Pageable))</li>
 *   <li>Flushing the persistence context</li>
 * </ul>
 * You get ~15 free methods: save(), findById(), findAll(), deleteById(), count(), etc.
 * Zero SQL written for standard CRUD.
 *
 * <p><b>Why Optional return type?</b>
 * {@code Optional<User>} forces the caller to explicitly handle the "not found" case.
 * If we returned a nullable {@code User}, callers might forget the null check and
 * get a NullPointerException at runtime. Optional makes the absent case explicit
 * at compile time — this is why the service layer calls .orElseThrow().
 *
 * <p><b>How does findByEmail work?</b>
 * Spring Data JPA reads the method name and generates the SQL automatically:
 * "findBy" + "Email" → SELECT * FROM users WHERE email = ?
 * This is called "Query Derivation" — no @Query annotation needed for simple lookups.
 *
 * <p><b>Why existsByEmail?</b>
 * For a duplicate-check before registration, we only need to know IF a user exists —
 * not fetch the whole object. existsByEmail() generates:
 * SELECT COUNT(*) > 0 FROM users WHERE email = ?
 * Much more efficient than fetching the full User just to check existence.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user by their email address (used for login and duplicate checks).
     *
     * @param email the email to search for
     * @return an Optional containing the User if found, or empty if not
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a user with the given email already exists (used at registration).
     *
     * @param email the email to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}

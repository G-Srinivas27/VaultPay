package com.srinivas.vaultpay.user.service;

import com.srinivas.vaultpay.user.dto.RegisterRequest;
import com.srinivas.vaultpay.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface defining the contract for all user-related business operations.
 *
 * <p>Controllers and any other callers depend on THIS interface, never on the
 * concrete implementation. This is the Dependency Inversion Principle in action.
 *
 * <p>Benefits:
 * <ul>
 *   <li>Easy to mock in unit tests (Mockito.mock(UserService.class))</li>
 *   <li>The implementation can change (add caching, logging, etc.) without
 *       touching the controller</li>
 *   <li>Clear, documentation-ready API contract</li>
 * </ul>
 */
public interface UserService {

    /**
     * Registers a new user with the provided details.
     *
     * <p>Business rules enforced:
     * <ul>
     *   <li>Email must be unique — throws DuplicateResourceException if taken</li>
     *   <li>Password is BCrypt-hashed before storage</li>
     *   <li>Default role is USER</li>
     * </ul>
     *
     * @param request the registration payload
     * @return the newly created user (without password)
     * @throws com.srinivas.vaultpay.common.exception.DuplicateResourceException if email already exists
     */
    UserResponse registerUser(RegisterRequest request);

    /**
     * Retrieves a user by their unique ID.
     *
     * @param id the user's database ID
     * @return the user data
     * @throws com.srinivas.vaultpay.common.exception.ResourceNotFoundException if no user with this ID exists
     */
    UserResponse getUserById(Long id);

    /**
     * Retrieves a page of users.
     *
     * <p><b>Pageable</b> carries three pieces of information:
     * <ul>
     *   <li>page number  — which page to fetch (0-indexed)</li>
     *   <li>page size    — how many items per page</li>
     *   <li>sort         — optional field + direction (e.g., createdAt DESC)</li>
     * </ul>
     * Spring Data translates this into: SELECT ... LIMIT {size} OFFSET {page * size}
     * plus a separate COUNT(*) query to compute totalElements.
     *
     * @param pageable pagination and sorting parameters
     * @return a Page containing the current slice of users and metadata
     */
    Page<UserResponse> getAllUsers(Pageable pageable);

    /**
     * Returns the profile of the currently authenticated user.
     * Reads the email from Spring's SecurityContext (set by JwtAuthenticationFilter).
     *
     * @param email the authenticated user's email (from JWT)
     * @return the user's own profile data
     */
    UserResponse getCurrentUser(String email);

    /**
     * Updates a user's role. ADMIN-only operation.
     *
     * @param id   the target user's ID
     * @param role the new role to assign (USER or ADMIN)
     * @return the updated user response
     * @throws com.srinivas.vaultpay.common.exception.ResourceNotFoundException if user not found
     */
    UserResponse updateUserRole(Long id, com.srinivas.vaultpay.user.entity.Role role);

    /**
     * Activates or deactivates a user account. ADMIN-only operation.
     *
     * @param id     the target user's ID
     * @param active true to activate, false to deactivate
     * @return the updated user response
     * @throws com.srinivas.vaultpay.common.exception.ResourceNotFoundException if user not found
     */
    UserResponse updateUserStatus(Long id, boolean active);

    /**
     * Updates the authenticated user's profile (name and phone).
     *
     * @param email   the authenticated user's email
     * @param request the profile update payload
     * @return the updated user response
     */
    UserResponse updateProfile(String email, com.srinivas.vaultpay.user.dto.UpdateProfileRequest request);
}


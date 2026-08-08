package com.srinivas.vaultpay.user.service;

import com.srinivas.vaultpay.common.exception.DuplicateResourceException;
import com.srinivas.vaultpay.common.exception.ResourceNotFoundException;
import com.srinivas.vaultpay.user.dto.RegisterRequest;
import com.srinivas.vaultpay.user.dto.UserResponse;
import com.srinivas.vaultpay.user.entity.User;
import com.srinivas.vaultpay.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.srinivas.vaultpay.email.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Implementation of {@link UserService} containing all user-related business logic.
 *
 * <p><b>@Service</b> — marks this as a Spring-managed component. Spring creates
 * a single instance (singleton scope by default) and injects it wherever UserService is needed.
 *
 * <p><b>Constructor injection</b> — all dependencies are declared as final fields and
 * injected via the constructor. This is the recommended approach because:
 * <ul>
 *   <li>Dependencies are guaranteed non-null at construction time</li>
 *   <li>The class can be unit-tested without a Spring context (just call new UserServiceImpl(...))</li>
 *   <li>Immutability — final fields can't be accidentally reassigned</li>
 *   <li>Makes circular dependencies fail at startup, not at runtime</li>
 * </ul>
 * Lombok's {@code @RequiredArgsConstructor} generates this constructor automatically
 * for all final fields — so we don't need to write @Autowired anywhere.
 */
@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    // Constructor injection
    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    /**
     * Registers a new user.
     *
     * <p><b>@Transactional</b> — wraps this method in a DB transaction.
     * If anything fails mid-way (e.g., a DB error after the duplicate check),
     * the entire operation rolls back. The entity is never left in a partial state.
     *
     * <p>Flow:
     * 1. Check email uniqueness → throw DuplicateResourceException if taken
     * 2. Hash the password with BCrypt
     * 3. Build and save the User entity
     * 4. Map to UserResponse and return (password excluded)
     */
    @Override
    @Transactional
    public UserResponse registerUser(RegisterRequest request) {
        log.debug("Registering new user with email: {}", request.email());

        // Step 1: Enforce email uniqueness at the application layer
        // (DB constraint is a second line of defence, not the first)
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        // Step 2: Build the entity using the Builder pattern — clear, readable, no positional argument confusion
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))  // BCrypt hash, NEVER raw
                .build();
        // Note: role defaults to USER and active defaults to true via @Builder.Default in entity

        // Step 3: Persist and return
        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {} and email: {}", savedUser.getId(), savedUser.getEmail());

        // Step 4: Send welcome email asynchronously
        emailService.sendRegistrationEmail(savedUser);

        return UserResponse.from(savedUser);
    }

    /**
     * Retrieves a user by ID.
     *
     * <p><b>@Transactional(readOnly = true)</b> — a critical production optimization.
     * For read-only operations, Spring:
     * <ul>
     *   <li>Skips dirty checking (Hibernate won't scan all entities for changes)</li>
     *   <li>Allows the database to use read replicas if configured</li>
     *   <li>Improves performance under load</li>
     * </ul>
     * Always mark read-only queries with readOnly = true.
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        return UserResponse.from(user);
    }

    /**
     * Returns a page of users sorted and filtered via Pageable.
     *
     * <p><b>Page.map() — the key method:</b>
     * {@code repository.findAll(pageable)} returns {@code Page<User>} (entity objects).
     * We must NEVER return entities from the API — so we map each User to UserResponse.
     * {@code Page.map()} does this while PRESERVING the pagination metadata (totalPages,
     * totalElements, etc.). It's the paginated equivalent of stream().map().
     *
     * <p>Spring Data generates TWO SQL queries under the hood:
     * <ol>
     *   <li>SELECT * FROM users LIMIT ? OFFSET ?  — the actual data</li>
     *   <li>SELECT COUNT(*) FROM users             — for totalElements metadata</li>
     * </ol>
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        log.debug("Fetching users — page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());

        return userRepository.findAll(pageable)   // Page<User> — Spring handles LIMIT + OFFSET + COUNT
                .map(UserResponse::from);          // Page<UserResponse> — preserves metadata, maps content
    }

    /**
     * Returns the currently authenticated user's own profile.
     *
     * <p>The email comes from the JWT — extracted by JwtAuthenticationFilter and
     * stored in SecurityContext. The controller reads it from the Authentication object.
     * This ensures users can ONLY see their own profile, never someone else's.
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        log.debug("Fetching own profile for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return UserResponse.from(user);
    }

    /**
     * Promotes or demotes a user's role. Admin-only.
     *
     * <p>Use case: after reviewing a team member, an admin grants them ADMIN access.
     * Or revokes it: demote ADMIN → USER.
     */
    @Override
    @Transactional
    public UserResponse updateUserRole(Long id, com.srinivas.vaultpay.user.entity.Role role) {
        log.debug("Updating role for user ID: {} to: {}", id, role);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setRole(role);
        User savedUser = userRepository.save(user);

        log.info("Role updated for user ID: {} — new role: {}", id, role);
        return UserResponse.from(savedUser);
    }

    /**
     * Activates or deactivates a user account. Admin-only.
     *
     * <p>Deactivation is preferred over deletion — it preserves the audit trail
     * while preventing the user from logging in.
     * A deactivated user's JWT is immediately rejected because CustomUserDetailsService
     * sets accountLocked = !user.isActive() — Spring Security blocks locked accounts.
     */
    @Override
    @Transactional
    public UserResponse updateUserStatus(Long id, boolean active) {
        log.debug("Updating status for user ID: {} — active: {}", id, active);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        user.setActive(active);
        User savedUser = userRepository.save(user);

        log.info("Status updated for user ID: {} — active: {}", id, active);
        
        // Send async email notification
        emailService.sendStatusChangeEmail(savedUser, active);
        
        return UserResponse.from(savedUser);
    }

    /**
     * Updates the authenticated user's profile.
     */
    @Override
    @Transactional
    public UserResponse updateProfile(String email, com.srinivas.vaultpay.user.dto.UpdateProfileRequest request) {
        log.debug("Updating profile for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhoneNumber(request.phoneNumber());

        User savedUser = userRepository.save(user);

        log.info("Profile updated successfully for user ID: {}", savedUser.getId());
        return UserResponse.from(savedUser);
    }
}


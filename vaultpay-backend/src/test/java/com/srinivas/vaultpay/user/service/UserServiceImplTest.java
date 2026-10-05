package com.srinivas.vaultpay.user.service;

import com.srinivas.vaultpay.common.exception.DuplicateResourceException;
import com.srinivas.vaultpay.common.exception.ResourceNotFoundException;
import com.srinivas.vaultpay.user.dto.RegisterRequest;
import com.srinivas.vaultpay.user.dto.UserResponse;
import com.srinivas.vaultpay.user.entity.Role;
import com.srinivas.vaultpay.user.entity.User;
import com.srinivas.vaultpay.email.service.EmailService;
import com.srinivas.vaultpay.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

/**
 * Unit tests for {@link UserServiceImpl}.
 *
 * <p><b>What is a Unit Test?</b>
 * A unit test verifies a single class (the "unit") IN ISOLATION.
 * "Isolation" means we REPLACE all collaborators (UserRepository, PasswordEncoder)
 * with fakes called MOCKS. No real database, no Spring context, no HTTP — just pure Java.
 *
 * <p><b>Why mock the repository?</b>
 * If our test hit a real database:
 * <ul>
 *   <li>It would be 100x slower (network/disk I/O vs. in-memory)</li>
 *   <li>A flaky DB connection would fail our tests even if our code is correct</li>
 *   <li>Tests would depend on data state — fragile and order-dependent</li>
 * </ul>
 * Unit tests must be: FAST, ISOLATED, REPEATABLE. Mocks make this possible.
 *
 * <p><b>@ExtendWith(MockitoExtension.class)</b> — activates Mockito for this test class.
 * Without this, @Mock and @InjectMocks annotations would do nothing.
 *
 * <p><b>@Mock</b> — creates a fake implementation of the interface. By default,
 * all methods return empty values (null, 0, false, empty Optional, etc.)
 * We override specific behaviors using given(...).willReturn(...)
 *
 * <p><b>@InjectMocks</b> — creates a real instance of UserServiceImpl and injects
 * the @Mock fields into it via constructor injection. This is why constructor
 * injection is so important — Mockito can see and inject the dependencies clearly.
 *
 * <p><b>BDD style (given/when/then)</b> — Behavior-Driven Development naming.
 * Makes tests read like English sentences:
 * GIVEN a user exists / WHEN we call getUser / THEN we get the correct response
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private UserServiceImpl userService;

    // ─── Shared test data ────────────────────────────────────────────────────

    private RegisterRequest validRequest;
    private User savedUser;

    /**
     * @BeforeEach — runs before EVERY test method.
     * We reset test data here to ensure each test starts from a clean state.
     * Never share mutable state between tests — it causes false positives/negatives.
     */
    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest(
                "Srinivas",
                "Gooda",
                "srinivas@vaultpay.com",
                "securePass123"
        );

        savedUser = User.builder()
                .id(1L)
                .firstName("Srinivas")
                .lastName("Gooda")
                .email("srinivas@vaultpay.com")
                .password("$2a$10$hashedPasswordHere")
                .role(Role.USER)
                .active(true)
                .build();
    }

    // ─── registerUser tests ───────────────────────────────────────────────────

    /**
     * @Nested — groups related tests together under a descriptive label.
     * Makes the test report much more readable:
     *   UserService Unit Tests
     *     Register User
     *       ✅ should register user successfully when email is unique
     *       ❌ should throw DuplicateResourceException when email already exists
     */
    @Nested
    @DisplayName("Register User")
    class RegisterUserTests {

        @Test
        @DisplayName("should register user successfully when email is unique")
        void shouldRegisterUserSuccessfully() {
            // GIVEN
            given(userRepository.existsByEmail(validRequest.email())).willReturn(false);
            given(passwordEncoder.encode(validRequest.password())).willReturn("$2a$10$hashedPasswordHere");
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // WHEN
            UserResponse response = userService.registerUser(validRequest);

            // THEN — assert the response contains correct data
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.firstName()).isEqualTo("Srinivas");
            assertThat(response.lastName()).isEqualTo("Gooda");
            assertThat(response.email()).isEqualTo("srinivas@vaultpay.com");
            assertThat(response.role()).isEqualTo(Role.USER);
            assertThat(response.active()).isTrue();

            // THEN — verify interactions: password was encoded, user was saved
            then(passwordEncoder).should().encode("securePass123");
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("should throw DuplicateResourceException when email already exists")
        void shouldThrowExceptionWhenEmailAlreadyExists() {
            // GIVEN — simulate email already in use
            given(userRepository.existsByEmail(validRequest.email())).willReturn(true);

            // WHEN + THEN — assertThatThrownBy is the clean JUnit 5 + AssertJ way
            // to verify exceptions without try-catch boilerplate
            assertThatThrownBy(() -> userService.registerUser(validRequest))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("srinivas@vaultpay.com");

            // THEN — verify we never tried to save or encode anything
            // (short-circuit on duplicate check is a business requirement)
            then(passwordEncoder).should(never()).encode(anyString());
            then(userRepository).should(never()).save(any(User.class));
        }
    }

    // ─── getUserById tests ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Get User By ID")
    class GetUserByIdTests {

        @Test
        @DisplayName("should return user when ID exists")
        void shouldReturnUserWhenIdExists() {
            // GIVEN
            given(userRepository.findById(1L)).willReturn(Optional.of(savedUser));

            // WHEN
            UserResponse response = userService.getUserById(1L);

            // THEN
            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.email()).isEqualTo("srinivas@vaultpay.com");

            then(userRepository).should().findById(1L);
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when ID does not exist")
        void shouldThrowExceptionWhenUserNotFound() {
            // GIVEN — empty Optional simulates "no user found"
            given(userRepository.findById(99L)).willReturn(Optional.empty());

            // WHEN + THEN
            assertThatThrownBy(() -> userService.getUserById(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ─── getAllUsers tests ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Get All Users")
    class GetAllUsersTests {

        @Test
        @DisplayName("should return all users as response DTOs")
        void shouldReturnAllUsers() {
            // GIVEN
            User secondUser = User.builder()
                    .id(2L)
                    .firstName("Jane")
                    .lastName("Doe")
                    .email("jane@vaultpay.com")
                    .password("$2a$10$anotherHash")
                    .role(Role.USER)
                    .active(true)
                    .build();

            // PageRequest.of(page, size) — creates a Pageable for testing
            // PageImpl — Spring Data's concrete Page implementation, used in tests
            // to simulate what the repository would return
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> userPage = new PageImpl<>(List.of(savedUser, secondUser), pageable, 2);

            given(userRepository.findAll(pageable)).willReturn(userPage);

            // WHEN
            Page<UserResponse> responses = userService.getAllUsers(pageable);

            // THEN — assert on the Page content
            assertThat(responses.getContent()).hasSize(2);
            assertThat(responses.getTotalElements()).isEqualTo(2);
            assertThat(responses.getContent()).extracting(UserResponse::email)
                    .containsExactlyInAnyOrder("srinivas@vaultpay.com", "jane@vaultpay.com");
        }

        @Test
        @DisplayName("should return empty page when no users exist")
        void shouldReturnEmptyPageWhenNoUsers() {
            // GIVEN
            Pageable pageable = PageRequest.of(0, 10);
            Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);

            given(userRepository.findAll(pageable)).willReturn(emptyPage);

            // WHEN
            Page<UserResponse> responses = userService.getAllUsers(pageable);

            // THEN
            assertThat(responses.getContent()).isNotNull().isEmpty();
            assertThat(responses.getTotalElements()).isZero();
        }
    }
}

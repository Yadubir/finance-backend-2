package com.finance.service;

import com.finance.dto.request.CreateUserRequest;
import com.finance.dto.request.UpdateUserRequest;
import com.finance.dto.response.UserResponse;
import com.finance.exception.BusinessException;
import com.finance.exception.DuplicateResourceException;
import com.finance.exception.ResourceNotFoundException;
import com.finance.model.Role;
import com.finance.model.User;
import com.finance.model.UserStatus;
import com.finance.repository.UserRepository;
import com.finance.security.UserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceTest {

    @Mock UserRepository  userRepository;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks UserService userService;

    private User adminUser;
    private User analystUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L).email("admin@finance.com").fullName("Admin")
                .passwordHash("hashed").role(Role.ADMIN).status(UserStatus.ACTIVE)
                .build();

        analystUser = User.builder()
                .id(2L).email("analyst@finance.com").fullName("Analyst")
                .passwordHash("hashed").role(Role.ANALYST).status(UserStatus.ACTIVE)
                .build();

        // Mock SecurityContextHolder so service can call getCurrentPrincipal()
        mockSecurityContext(adminUser);
    }

    // ── createUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createUser() creates user successfully with hashed password")
    void createUser_validRequest_createsAndReturnsUser() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("newuser@finance.com");
        req.setFullName("New User");
        req.setPassword("NewUser@123");
        req.setRole(Role.VIEWER);

        when(userRepository.existsByEmail("newuser@finance.com")).thenReturn(false);
        when(passwordEncoder.encode("NewUser@123")).thenReturn("$hashed$");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        UserResponse response = userService.createUser(req);

        assertThat(response.getEmail()).isEqualTo("newuser@finance.com");
        assertThat(response.getRole()).isEqualTo(Role.VIEWER);
        verify(passwordEncoder).encode("NewUser@123");
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("$hashed$")));
    }

    @Test
    @DisplayName("createUser() throws DuplicateResourceException if email already exists")
    void createUser_duplicateEmail_throwsDuplicateResourceException() {
        CreateUserRequest req = new CreateUserRequest();
        req.setEmail("admin@finance.com");
        req.setFullName("Duplicate");
        req.setPassword("Pass@1234");
        req.setRole(Role.VIEWER);

        when(userRepository.existsByEmail("admin@finance.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(req))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("admin@finance.com");

        verify(userRepository, never()).save(any());
    }

    // ── getUserById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getUserById() returns user when found")
    void getUserById_existingId_returnsUser() {
        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(adminUser));

        UserResponse response = userService.getUserById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("admin@finance.com");
    }

    @Test
    @DisplayName("getUserById() throws ResourceNotFoundException for unknown id")
    void getUserById_unknownId_throwsNotFoundException() {
        when(userRepository.findActiveById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── updateUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateUser() applies only non-null fields")
    void updateUser_partialRequest_updatesOnlyProvidedFields() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("Updated Name");
        // role and status left null — should not change

        when(userRepository.findActiveById(2L)).thenReturn(Optional.of(analystUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.updateUser(2L, req);

        assertThat(response.getFullName()).isEqualTo("Updated Name");
        assertThat(response.getRole()).isEqualTo(Role.ANALYST); // unchanged
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE); // unchanged
    }

    @Test
    @DisplayName("updateUser() throws BusinessException when demoting last active admin")
    void updateUser_demoteLastAdmin_throwsBusinessException() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setRole(Role.VIEWER); // demoting the last admin

        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(adminUser));
        // Only 1 active admin exists
        Page<User> singleAdminPage = new PageImpl<>(java.util.List.of(adminUser));
        when(userRepository.findAllActive(eq(Role.ADMIN), eq(UserStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(singleAdminPage);

        assertThatThrownBy(() -> userService.updateUser(1L, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("last active admin");
    }

    // ── deleteUser ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteUser() sets deletedAt on target user")
    void deleteUser_validTarget_softDeletesUser() {
        when(userRepository.findActiveById(2L)).thenReturn(Optional.of(analystUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        userService.deleteUser(2L);

        verify(userRepository).save(argThat(u -> u.getDeletedAt() != null));
    }

    @Test
    @DisplayName("deleteUser() throws BusinessException when trying to delete own account")
    void deleteUser_ownAccount_throwsBusinessException() {
        // The mocked security context has adminUser (id=1) as current user
        // Trying to delete id=1 (own account)
        when(userRepository.findActiveById(1L)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> userService.deleteUser(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("own account");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void mockSecurityContext(User user) {
        UserPrincipal principal   = new UserPrincipal(user);
        Authentication auth       = mock(Authentication.class);
        SecurityContext context   = mock(SecurityContext.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(context.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(context);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }
}

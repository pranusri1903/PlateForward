package com.plateforward.user;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(User.Role role);
    List<User> findByRoleOrderByVerifiedAscIdDesc(User.Role role);
    List<User> findByRole(User.Role role);
    List<User> findByRoleInAndVerifiedTrueAndAreaIgnoreCase(Collection<User.Role> roles, String area);
}

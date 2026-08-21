package com.foliopath360.lms.repository;

import com.foliopath360.lms.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByMobileNumber(String mobileNumber);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(
            String username,
            String email
    );

    Optional<User> findBySetupToken(String setupToken);

    List<User> findTop10ByOrderByCreatedDtDesc();

    long countByRolesRoleName(String roleName);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = 'STUDENT' " +
            "ORDER BY u.createdDt DESC")
    List<User> findAllStudents();

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = 'STUDENT' AND " +
            "(LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "ORDER BY u.createdDt DESC")
    List<User> searchStudents(@Param("search") String search);
}

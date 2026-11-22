
package com.khadar.welcomepage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // =====================================
    // EMAIL
    // =====================================

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    // =====================================
    // MOBILE NUMBER
    // =====================================

    boolean existsByMobileNumber(String mobileNumber);

    Optional<User> findByMobileNumber(String mobileNumber);

    // =====================================
    // MOBILE VERIFICATION
    // =====================================

    Optional<User> findByMobileNumberAndMobileVerifiedTrue(
            String mobileNumber
    );
}

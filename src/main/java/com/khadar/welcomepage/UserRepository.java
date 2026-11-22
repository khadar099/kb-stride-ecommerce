
package com.khadar.welcomepage;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Email operations

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByEmailIgnoreCase(String email);

    // Mobile number operations

    boolean existsByMobileNumber(String mobileNumber);

    Optional<User> findByMobileNumber(String mobileNumber);

    // Only verified mobile accounts can use OTP login

    Optional<User> findByMobileNumberAndMobileVerifiedTrue(
            String mobileNumber
    );
}

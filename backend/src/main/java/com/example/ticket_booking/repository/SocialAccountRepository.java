package com.example.ticket_booking.repository;

import com.example.ticket_booking.domain.SocialAccount;
import com.example.ticket_booking.domain.SocialProvider;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

  Optional<SocialAccount> findByProviderAndProviderUserId(
      SocialProvider provider, String providerUserId);
}

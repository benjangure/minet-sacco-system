package com.minet.sacco.repository;

import com.minet.sacco.entity.MemberCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MemberCredentialRepository extends JpaRepository<MemberCredential, Long> {
    
    Optional<MemberCredential> findByMemberId(Long memberId);
    
    Optional<MemberCredential> findByUsername(String username);
    
    List<MemberCredential> findByEmailSentFalse();
    
    List<MemberCredential> findByPasswordChangedFalse();
    
    @Query("SELECT mc FROM MemberCredential mc WHERE mc.emailSent = false AND mc.email IS NOT NULL AND mc.email != ''")
    List<MemberCredential> findPendingEmailDeliveries();
    
    @Query("SELECT mc FROM MemberCredential mc WHERE mc.passwordChanged = false")
    List<MemberCredential> findMembersAwaitingPasswordSetup();
    
    @Query("SELECT COUNT(mc) FROM MemberCredential mc WHERE mc.emailSent = true")
    long countEmailsSent();
    
    @Query("SELECT COUNT(mc) FROM MemberCredential mc WHERE mc.passwordChanged = true")
    long countPasswordsChanged();
    
    List<MemberCredential> findByMemberNameContainingIgnoreCaseOrUsernameContainingIgnoreCase(String memberName, String username);
}
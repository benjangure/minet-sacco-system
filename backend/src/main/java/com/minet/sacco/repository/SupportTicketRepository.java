package com.minet.sacco.repository;

import com.minet.sacco.entity.SupportTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    List<SupportTicket> findByMemberIdOrderByCreatedAtDesc(Long memberId);

    List<SupportTicket> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    List<SupportTicket> findByStatusOrderByCreatedAtDesc(SupportTicket.Status status);

    @Query("SELECT st FROM SupportTicket st WHERE st.member.id = :memberId AND st.status = :status ORDER BY st.createdAt DESC")
    List<SupportTicket> findByMemberIdAndStatus(@Param("memberId") Long memberId, @Param("status") SupportTicket.Status status);

    @Query("SELECT COUNT(st) FROM SupportTicket st WHERE st.status = :status")
    Long countByStatus(@Param("status") SupportTicket.Status status);
}

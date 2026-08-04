package com.minet.sacco.repository;

import com.minet.sacco.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMemberNumber(String memberNumber);

    Optional<Member> findByEmployeeId(String employeeId);

    boolean existsByEmployeeId(String employeeId);

    Optional<Member> findByNationalId(String nationalId);

    Optional<Member> findByEmail(String email);

    List<Member> findByStatus(Member.Status status);

    List<Member> findByDepartment(String department);

    boolean existsByMemberNumber(String memberNumber);

    boolean existsByNationalId(String nationalId);

    boolean existsByEmail(String email);

    /**
     * Optimized query to find over-committed guarantors in a single database call
     * Returns: memberId, memberNumber, memberName, memberStatus, totalSavings, 
     *          frozenSelfGuarantee, frozenPledges, availableSavings, amountOverCommitted
     */
    @Query(value = """
        SELECT 
            m.id as memberId,
            m.member_number as memberNumber,
            CONCAT(m.first_name, ' ', m.last_name) as memberName,
            m.status as memberStatus,
            COALESCE(savings.total_savings, 0) as totalSavings,
            COALESCE(self_g.frozen_self, 0) as frozenSelfGuarantee,
            COALESCE(other_g.frozen_pledges, 0) as frozenPledges,
            GREATEST(COALESCE(savings.total_savings, 0) - COALESCE(self_g.frozen_self, 0), 0) as availableSavings,
            GREATEST(COALESCE(other_g.frozen_pledges, 0) - 
                GREATEST(COALESCE(savings.total_savings, 0) - COALESCE(self_g.frozen_self, 0), 0), 0) as amountOverCommitted
        FROM members m
        LEFT JOIN (
            SELECT member_id, SUM(balance) as total_savings
            FROM accounts
            WHERE account_type = 'SAVINGS'
            GROUP BY member_id
        ) savings ON savings.member_id = m.id
        LEFT JOIN (
            SELECT member_id, SUM(pledge_amount) as frozen_self
            FROM guarantors
            WHERE self_guarantee = true AND status = 'ACTIVE'
            GROUP BY member_id
        ) self_g ON self_g.member_id = m.id
        LEFT JOIN (
            SELECT member_id, SUM(pledge_amount) as frozen_pledges
            FROM guarantors
            WHERE self_guarantee = false AND status = 'ACTIVE'
            GROUP BY member_id
        ) other_g ON other_g.member_id = m.id
        WHERE COALESCE(other_g.frozen_pledges, 0) > 
              GREATEST(COALESCE(savings.total_savings, 0) - COALESCE(self_g.frozen_self, 0), 0)
        ORDER BY amountOverCommitted DESC
        """, nativeQuery = true)
    List<Object[]> findOverCommittedGuarantors();
}
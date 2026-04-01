package com.minet.sacco.repository;

import com.minet.sacco.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
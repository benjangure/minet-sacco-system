package com.minet.sacco.repository;

import com.minet.sacco.entity.FundConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FundConfigurationRepository extends JpaRepository<FundConfiguration, Long> {
    
    List<FundConfiguration> findByEnabledTrueOrderByDisplayOrderAsc();
    
    List<FundConfiguration> findAllByOrderByDisplayOrderAsc();
    
    Optional<FundConfiguration> findByFundType(String fundType);
}

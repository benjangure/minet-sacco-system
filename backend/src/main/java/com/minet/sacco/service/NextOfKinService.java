package com.minet.sacco.service;

import com.minet.sacco.dto.NextOfKinDTO;
import com.minet.sacco.entity.Member;
import com.minet.sacco.entity.NextOfKin;
import com.minet.sacco.repository.MemberRepository;
import com.minet.sacco.repository.NextOfKinRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class NextOfKinService {
    
    private static final Logger log = LoggerFactory.getLogger(NextOfKinService.class);
    
    @Autowired
    private NextOfKinRepository nextOfKinRepository;
    
    @Autowired
    private MemberRepository memberRepository;
    
    public List<NextOfKinDTO> getNextOfKinByMemberId(Long memberId) {
        List<NextOfKin> nextOfKinList = nextOfKinRepository.findByMemberId(memberId);
        return nextOfKinList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public List<NextOfKinDTO> saveNextOfKinList(Long memberId, List<NextOfKinDTO> nextOfKinDTOs) {
        // Validate member exists
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found with ID: " + memberId));
        
        // Validate total percentage = 100
        BigDecimal totalPercentage = nextOfKinDTOs.stream()
                .map(NextOfKinDTO::getPercentage)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalPercentage.compareTo(new BigDecimal("100")) != 0) {
            throw new RuntimeException("Total percentage must equal 100%. Current total: " + totalPercentage + "%");
        }
        
        // Delete existing next of kin for this member
        nextOfKinRepository.deleteByMemberId(memberId);
        
        // Save new list
        List<NextOfKin> savedList = nextOfKinDTOs.stream()
                .map(dto -> {
                    NextOfKin nok = convertToEntity(dto);
                    nok.setMember(member);
                    return nextOfKinRepository.save(nok);
                })
                .collect(Collectors.toList());
        
        log.info("Saved {} next of kin for member {}", savedList.size(), memberId);
        
        return savedList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public NextOfKinDTO addNextOfKin(Long memberId, NextOfKinDTO dto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Member not found with ID: " + memberId));
        
        // Check if adding this would exceed 100%
        BigDecimal currentTotal = nextOfKinRepository.getTotalPercentageForMember(memberId);
        BigDecimal newTotal = currentTotal.add(dto.getPercentage());
        
        if (newTotal.compareTo(new BigDecimal("100")) > 0) {
            throw new RuntimeException(
                "Cannot add next of kin. Total percentage would be " + newTotal + "% (max 100%)"
            );
        }
        
        NextOfKin nok = convertToEntity(dto);
        nok.setMember(member);
        NextOfKin saved = nextOfKinRepository.save(nok);
        
        log.info("Added next of kin {} for member {}", saved.getId(), memberId);
        return convertToDTO(saved);
    }
    
    @Transactional
    public NextOfKinDTO updateNextOfKin(Long id, NextOfKinDTO dto) {
        NextOfKin existing = nextOfKinRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Next of Kin not found with ID: " + id));
        
        // Validate percentage change doesn't exceed 100%
        BigDecimal currentTotal = nextOfKinRepository.getTotalPercentageForMember(existing.getMember().getId());
        BigDecimal oldPercentage = existing.getPercentage();
        BigDecimal newPercentage = dto.getPercentage();
        BigDecimal newTotal = currentTotal.subtract(oldPercentage).add(newPercentage);
        
        if (newTotal.compareTo(new BigDecimal("100")) > 0) {
            throw new RuntimeException(
                "Cannot update. Total percentage would be " + newTotal + "% (max 100%)"
            );
        }
        
        existing.setFullName(dto.getFullName());
        existing.setRelationship(dto.getRelationship());
        existing.setPhone(dto.getPhone());
        existing.setEmail(dto.getEmail());
        existing.setIdNumber(dto.getIdNumber());
        existing.setPercentage(dto.getPercentage());
        existing.setIsPrimary(dto.getIsPrimary());
        
        NextOfKin updated = nextOfKinRepository.save(existing);
        log.info("Updated next of kin {}", id);
        return convertToDTO(updated);
    }
    
    @Transactional
    public void deleteNextOfKin(Long id) {
        if (!nextOfKinRepository.existsById(id)) {
            throw new RuntimeException("Next of Kin not found with ID: " + id);
        }
        nextOfKinRepository.deleteById(id);
        log.info("Deleted next of kin {}", id);
    }
    
    public BigDecimal getTotalPercentageForMember(Long memberId) {
        return nextOfKinRepository.getTotalPercentageForMember(memberId);
    }
    
    private NextOfKinDTO convertToDTO(NextOfKin entity) {
        NextOfKinDTO dto = new NextOfKinDTO();
        dto.setId(entity.getId());
        dto.setMemberId(entity.getMember().getId());
        dto.setFullName(entity.getFullName());
        dto.setRelationship(entity.getRelationship());
        dto.setPhone(entity.getPhone());
        dto.setEmail(entity.getEmail());
        dto.setIdNumber(entity.getIdNumber());
        dto.setPercentage(entity.getPercentage());
        dto.setIsPrimary(entity.getIsPrimary());
        return dto;
    }
    
    private NextOfKin convertToEntity(NextOfKinDTO dto) {
        NextOfKin entity = new NextOfKin();
        if (dto.getId() != null) {
            entity.setId(dto.getId());
        }
        entity.setFullName(dto.getFullName());
        entity.setRelationship(dto.getRelationship());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setIdNumber(dto.getIdNumber());
        entity.setPercentage(dto.getPercentage() != null ? dto.getPercentage() : BigDecimal.ZERO);
        entity.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false);
        return entity;
    }
}

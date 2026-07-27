package com.minet.sacco.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.repository.LoanProductRepository;
import com.minet.sacco.entity.LoanProduct;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/gl/data-sources")
@CrossOrigin(origins = "*", maxAge = 3600)
public class GLDataSourceController {
  private static final Logger logger = LoggerFactory.getLogger(GLDataSourceController.class);
  
  @Autowired
  private LoanProductRepository loanProductRepository;
  
  /**
   * Get all available auto-calculation data sources for GL account configuration
   */
  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN', 'TREASURER')")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getDataSources() {
    try {
      List<Map<String, Object>> sources = new ArrayList<>();
      
      // LOANS source
      Map<String, Object> loansSource = new LinkedHashMap<>();
      loansSource.put("sourceType", "LOANS");
      loansSource.put("label", "Loan Portfolio");
      loansSource.put("requiresProductSelection", true);
      
      // Get active loan products
      List<LoanProduct> allProducts = loanProductRepository.findAll();
      List<Map<String, Object>> productsData = new ArrayList<>();
      for (LoanProduct product : allProducts) {
        // Only include active products
        if (product.getIsActive() != null && product.getIsActive()) {
          Map<String, Object> productMap = new LinkedHashMap<>();
          productMap.put("id", product.getId());
          productMap.put("name", product.getName());
          productsData.add(productMap);
        }
      }
      loansSource.put("loanProducts", productsData);
      sources.add(loansSource);
      
      // SAVINGS source
      Map<String, Object> savingsSource = new LinkedHashMap<>();
      savingsSource.put("sourceType", "SAVINGS");
      savingsSource.put("label", "Member Savings");
      savingsSource.put("requiresProductSelection", false);
      sources.add(savingsSource);
      
      // SHARES source
      Map<String, Object> sharesSource = new LinkedHashMap<>();
      sharesSource.put("sourceType", "SHARES");
      sharesSource.put("label", "Member Shares");
      sharesSource.put("requiresProductSelection", false);
      sources.add(sharesSource);
      
      // TRANSACTIONS source
      Map<String, Object> transactionsSource = new LinkedHashMap<>();
      transactionsSource.put("sourceType", "TRANSACTIONS");
      transactionsSource.put("label", "Transactions");
      transactionsSource.put("requiresProductSelection", false);
      sources.add(transactionsSource);
      
      Map<String, Object> response = new LinkedHashMap<>();
      response.put("sources", sources);
      
      logger.info("Retrieved GL data sources with " + productsData.size() + " active loan products");
      return ResponseEntity.ok(ApiResponse.success("Data sources retrieved", response));
    } catch (Exception e) {
      logger.error("Error retrieving GL data sources", e);
      return ResponseEntity.badRequest()
        .body(ApiResponse.error("Failed to retrieve data sources: " + e.getMessage()));
    }
  }
}

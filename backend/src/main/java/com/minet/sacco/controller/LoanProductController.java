package com.minet.sacco.controller;

import com.minet.sacco.dto.ApiResponse;
import com.minet.sacco.entity.LoanProduct;
import com.minet.sacco.repository.LoanProductRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loan-products")
@CrossOrigin
public class LoanProductController {

    @Autowired
    private LoanProductRepository loanProductRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LoanProduct>>> getAllLoanProducts() {
        List<LoanProduct> products = loanProductRepository.findAll();
        return ResponseEntity.ok(ApiResponse.success("Loan products retrieved successfully", products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LoanProduct>> getLoanProductById(@PathVariable Long id) {
        return loanProductRepository.findById(id)
                .map(product -> ResponseEntity.ok(ApiResponse.success("Loan product found", product)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<LoanProduct>> createLoanProduct(@Valid @RequestBody LoanProduct loanProduct) {
        LoanProduct created = loanProductRepository.save(loanProduct);
        return ResponseEntity.ok(ApiResponse.success("Loan product created successfully", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<LoanProduct>> updateLoanProduct(
            @PathVariable Long id,
            @Valid @RequestBody LoanProduct loanProduct) {
        loanProduct.setId(id);
        LoanProduct updated = loanProductRepository.save(loanProduct);
        return ResponseEntity.ok(ApiResponse.success("Loan product updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteLoanProduct(@PathVariable Long id) {
        loanProductRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Loan product deleted successfully"));
    }
}





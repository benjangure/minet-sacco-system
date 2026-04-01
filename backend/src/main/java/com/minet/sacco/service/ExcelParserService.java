package com.minet.sacco.service;

import com.minet.sacco.entity.BulkTransactionItem;
import com.minet.sacco.entity.BulkMemberItem;
import com.minet.sacco.entity.BulkLoanItem;
import com.minet.sacco.entity.BulkDisbursementItem;
import com.minet.sacco.entity.FundConfiguration;
import com.minet.sacco.repository.FundConfigurationRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelParserService {

    @Autowired
    private FundConfigurationRepository fundConfigurationRepository;

    public List<BulkTransactionItem> parseMonthlyContributions(MultipartFile file) throws IOException {
        List<BulkTransactionItem> items = new ArrayList<>();
        List<FundConfiguration> enabledFunds = fundConfigurationRepository.findByEnabledTrueOrderByDisplayOrderAsc();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Skip header row
            int rowNumber = 1;
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Skip header
                
                BulkTransactionItem item = new BulkTransactionItem();
                item.setRowNumber(rowNumber++);
                
                int colIndex = 0;
                
                // Column 0: Member Number (always present)
                Cell memberCell = row.getCell(colIndex++);
                if (memberCell != null) {
                    item.setMemberNumber(getCellValueAsString(memberCell));
                }
                
                // Column 1: Savings (always present)
                Cell savingsCell = row.getCell(colIndex++);
                if (savingsCell != null) {
                    item.setSavingsAmount(getCellValueAsBigDecimal(savingsCell));
                }
                
                // Column 2: Shares (always present)
                Cell sharesCell = row.getCell(colIndex++);
                if (sharesCell != null) {
                    item.setSharesAmount(getCellValueAsBigDecimal(sharesCell));
                }
                
                // Column 3: Loan Repayment (always present)
                Cell loanRepaymentCell = row.getCell(colIndex++);
                if (loanRepaymentCell != null) {
                    item.setLoanRepaymentAmount(getCellValueAsBigDecimal(loanRepaymentCell));
                }
                
                // Column 4: Loan Number (always present)
                Cell loanNumberCell = row.getCell(colIndex++);
                if (loanNumberCell != null) {
                    item.setLoanNumber(getCellValueAsString(loanNumberCell));
                }
                
                // Dynamic columns based on enabled funds
                for (FundConfiguration fund : enabledFunds) {
                    Cell fundCell = row.getCell(colIndex++);
                    BigDecimal amount = fundCell != null ? getCellValueAsBigDecimal(fundCell) : BigDecimal.ZERO;
                    
                    switch (fund.getFundType()) {
                        case "BENEVOLENT_FUND":
                            item.setBenevolentFundAmount(amount);
                            break;
                        case "DEVELOPMENT_FUND":
                            item.setDevelopmentFundAmount(amount);
                            break;
                        case "SCHOOL_FEES":
                            item.setSchoolFeesAmount(amount);
                            break;
                        case "HOLIDAY_FUND":
                            item.setHolidayFundAmount(amount);
                            break;
                        case "EMERGENCY_FUND":
                            item.setEmergencyFundAmount(amount);
                            break;
                    }
                }
                
                // Only add if member number is present
                if (item.getMemberNumber() != null && !item.getMemberNumber().trim().isEmpty()) {
                    items.add(item);
                }
            }
        }
        
        return items;
    }
    public List<BulkMemberItem> parseMemberRegistration(MultipartFile file) throws IOException {
        List<BulkMemberItem> items = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            int rowNumber = 1;
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                
                BulkMemberItem item = new BulkMemberItem();
                item.setRowNumber(rowNumber++);
                
                item.setFirstName(getCellValueAsString(row.getCell(0)));
                item.setLastName(getCellValueAsString(row.getCell(1)));
                item.setEmail(getCellValueAsString(row.getCell(2)));
                item.setPhone(getCellValueAsString(row.getCell(3)));
                item.setNationalId(getCellValueAsString(row.getCell(4)));
                
                // Parse date of birth - handle Excel date cells and multiple formats
                Cell dobCell = row.getCell(5);
                if (dobCell != null) {
                    java.time.LocalDate parsedDate = parseDateCell(dobCell);
                    if (parsedDate != null) {
                        item.setDateOfBirth(parsedDate);
                    }
                }
                
                item.setDepartment(getCellValueAsString(row.getCell(6)));
                item.setEmployeeId(getCellValueAsString(row.getCell(7)));
                item.setEmployer(getCellValueAsString(row.getCell(8)));
                item.setBank(getCellValueAsString(row.getCell(9)));
                item.setBankAccount(getCellValueAsString(row.getCell(10)));
                item.setNextOfKin(getCellValueAsString(row.getCell(11)));
                item.setNokPhone(getCellValueAsString(row.getCell(12)));
                
                if (item.getFirstName() != null && !item.getFirstName().trim().isEmpty()) {
                    items.add(item);
                }
            }
        }
        
        return items;
    }
    
    private java.time.LocalDate parseDateCell(Cell cell) {
        if (cell == null) return null;
        
        try {
            // First, try to get the date directly if it's a DATE cell type
            if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                java.util.Date date = cell.getDateCellValue();
                if (date != null) {
                    return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                }
            }
            
            // DEFINITIVE APPROACH: Use DataFormatter to get the formatted date string
            // This is what Excel actually displays, which we can then parse
            org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
            String cellValue = formatter.formatCellValue(cell);
            
            if (cellValue == null || cellValue.trim().isEmpty()) {
                return null;
            }
            
            cellValue = cellValue.trim();
            
            // Try to parse the formatted value with multiple date formats
            java.time.format.DateTimeFormatter[] dateFormatters = {
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                java.time.format.DateTimeFormatter.ofPattern("M/d/yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("d-M-yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("M-d-yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("MM-dd-yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("d.M.yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"),
                java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"),
            };
            
            for (java.time.format.DateTimeFormatter dtf : dateFormatters) {
                try {
                    return java.time.LocalDate.parse(cellValue, dtf);
                } catch (Exception e) {
                    // Try next format
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public List<BulkLoanItem> parseLoanApplications(MultipartFile file) throws IOException {
        List<BulkLoanItem> items = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            int rowNumber = 1;
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                
                BulkLoanItem item = new BulkLoanItem();
                item.setRowNumber(rowNumber++);
                
                item.setMemberNumber(getCellValueAsString(row.getCell(0)));
                item.setLoanProductName(getCellValueAsString(row.getCell(1)));
                item.setAmount(getCellValueAsBigDecimal(row.getCell(2)));
                item.setTermMonths(getCellValueAsInteger(row.getCell(3)));
                item.setPurpose(getCellValueAsString(row.getCell(4)));
                item.setGuarantor1(getCellValueAsString(row.getCell(5)));
                item.setGuarantor2(getCellValueAsString(row.getCell(6)));
                item.setGuarantor3(getCellValueAsString(row.getCell(7)));
                
                if (item.getMemberNumber() != null && !item.getMemberNumber().trim().isEmpty()) {
                    items.add(item);
                }
            }
        }
        
        return items;
    }
    
    public List<BulkDisbursementItem> parseLoanDisbursements(MultipartFile file) throws IOException {
        List<BulkDisbursementItem> items = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            
            int rowNumber = 1;
            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;
                
                BulkDisbursementItem item = new BulkDisbursementItem();
                item.setRowNumber(rowNumber++);
                
                item.setLoanNumber(getCellValueAsString(row.getCell(0)));
                item.setDisbursementAmount(getCellValueAsBigDecimal(row.getCell(1)));
                item.setDisbursementAccount(getCellValueAsString(row.getCell(2)));
                
                if (item.getLoanNumber() != null && !item.getLoanNumber().trim().isEmpty()) {
                    items.add(item);
                }
            }
        }
        
        return items;
    }
    
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                // Check if it's a date
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    java.util.Date date = cell.getDateCellValue();
                    java.time.LocalDate localDate = date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                    return localDate.toString(); // Returns YYYY-MM-DD format
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    private BigDecimal getCellValueAsBigDecimal(Cell cell) {
        if (cell == null) return BigDecimal.ZERO;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) return BigDecimal.ZERO;
                    return new BigDecimal(value);
                default:
                    return BigDecimal.ZERO;
            }
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
    
    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        
        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) return null;
                    return Integer.parseInt(value);
                default:
                    return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}

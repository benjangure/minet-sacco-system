package com.minet.sacco.config;

import com.minet.sacco.entity.KycDocument;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * Custom converter for KycDocument.DocumentType enum
 * Handles case-insensitive conversion from string to enum
 */
@Component
public class DocumentTypeConverter implements Converter<String, KycDocument.DocumentType> {
    
    @Override
    public KycDocument.DocumentType convert(String source) {
        if (source == null || source.isEmpty()) {
            throw new IllegalArgumentException("Document type cannot be null or empty");
        }
        
        try {
            return KycDocument.DocumentType.valueOf(source.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid document type: " + source + 
                ". Valid types are: NATIONAL_ID, PASSPORT, PASSPORT_PHOTO, APPLICATION_LETTER, KRA_PIN_CERTIFICATE"
            );
        }
    }
}

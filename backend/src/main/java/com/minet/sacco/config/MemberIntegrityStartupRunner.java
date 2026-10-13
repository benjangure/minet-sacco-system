package com.minet.sacco.config;

import com.minet.sacco.service.MemberCredentialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * On startup, repairs member data so that:
 *  - every member has a login + Member Credentials record
 *  - stored names are in Proper Case
 * Idempotent and non-fatal. Disable with app.member-integrity.on-startup=false.
 */
@Component
public class MemberIntegrityStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MemberIntegrityStartupRunner.class);

    @Autowired
    private MemberCredentialService memberCredentialService;

    @Value("${app.member-integrity.on-startup:true}")
    private boolean enabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            int renamed = memberCredentialService.normalizeStoredNames();
            Map<String, Object> sync = memberCredentialService.syncMissingCredentials(null);
            log.info("Member integrity check complete: {} names normalised, credential sync {}", renamed, sync);
        } catch (Exception e) {
            log.error("Member integrity check failed (non-fatal): {}", e.getMessage(), e);
        }
    }
}
